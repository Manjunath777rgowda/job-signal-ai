package com.jobsignal.ai.ingestion;

import com.jobsignal.ai.scanlog.ScanLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Allows manually triggering a job collection cycle via HTTP.
 *
 * POST /api/v1/collect         — fires and waits; returns summary JSON
 * POST /api/v1/collect/stream  — Server-Sent Events; emits one event per company
 *                                as it completes, then a final "done" event
 */
@RestController
@RequestMapping("/api/v1")
public class CollectController {

    private static final Logger log = LoggerFactory.getLogger(CollectController.class);

    private final JobCollectorRegistry registry;
    private final JobIngestionService  ingestionService;
    private final ScanLogService       scanLogService;

    public CollectController(JobCollectorRegistry registry,
                             JobIngestionService ingestionService,
                             ScanLogService scanLogService) {
        this.registry         = registry;
        this.ingestionService = ingestionService;
        this.scanLogService   = scanLogService;
    }

    /** Original fire-and-wait endpoint (used by scheduler health-check, backward compat). */
    @PostMapping("/collect")
    public ResponseEntity<Map<String, Object>> collect() {
        log.info("Manual job collection triggered via API");
        try {
            JobCollectorRegistry.CollectResult result = registry.collectAll("MANUAL");
            if (result == null) {
                log.info("Manual collect rejected — a scan is already in progress");
                return ResponseEntity.status(409)
                        .body(Map.of("status", "busy", "message", "A scan is already in progress"));
            }
            JobIngestionService.IngestResult ingest = ingestionService.ingest(result.jobs());
            scanLogService.finalize(result.scanLogId(), Instant.now(),
                    result.perCompany(), ingest.ingestedPerCompany(), ingest.totalCreated());
            return ResponseEntity.ok(Map.of(
                    "status",    "ok",
                    "collected", result.jobs().size(),
                    "ingested",  ingest.totalCreated()
            ));
        } catch (Exception e) {
            log.error("Manual collection failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Streaming collect via Server-Sent Events.
     *
     * Emits events while collection is running:
     *   event: company
     *   data: {"company":"Google","fetched":10}
     *
     * When all companies are done and ingestion is complete:
     *   event: done
     *   data: {"collected":78,"ingested":18}
     *
     * If already running:
     *   event: busy
     *   data: {"message":"A scan is already in progress"}
     */
    @PostMapping(value = "/collect/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter collectStream() {
        // Long timeout — a full scan with 32 companies can take several minutes
        SseEmitter emitter = new SseEmitter(600_000L);

        Thread.ofVirtual().start(() -> {
            try {
                if (registry.isRunning()) {
                    emitter.send(SseEmitter.event()
                            .name("busy")
                            .data("{\"message\":\"A scan is already in progress\"}"));
                    emitter.complete();
                    return;
                }

                // Kick off collection with a per-company callback that pushes SSE events
                JobCollectorRegistry.CollectResult result = registry.collectAll("MANUAL",
                        (company, fetched) -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("company")
                                        .data("{\"company\":\"" + escape(company)
                                              + "\",\"fetched\":" + fetched + "}"));
                            } catch (IOException e) {
                                log.debug("SSE send failed for company {}: {}", company, e.getMessage());
                            }
                        });

                if (result == null) {
                    emitter.send(SseEmitter.event()
                            .name("busy")
                            .data("{\"message\":\"A scan is already in progress\"}"));
                    emitter.complete();
                    return;
                }

                // Ingest and finalize
                JobIngestionService.IngestResult ingest = ingestionService.ingest(result.jobs());
                scanLogService.finalize(result.scanLogId(), Instant.now(),
                        result.perCompany(), ingest.ingestedPerCompany(), ingest.totalCreated());

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"collected\":" + result.jobs().size()
                              + ",\"ingested\":" + ingest.totalCreated() + "}"));
                emitter.complete();

            } catch (Exception e) {
                log.error("Streaming collect failed", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":\"" + escape(e.getMessage()) + "\"}"));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /** Minimal JSON string escaping for inline data strings. */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
