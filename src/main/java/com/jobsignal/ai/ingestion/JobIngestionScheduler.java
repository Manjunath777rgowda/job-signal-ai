package com.jobsignal.ai.ingestion;

import com.jobsignal.ai.scanlog.ScanLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Triggers job collection on startup (after a 10-second delay to let the DB settle)
 * and then every 6 hours.
 */
@Component
public class JobIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobIngestionScheduler.class);

    private final JobCollectorRegistry registry;
    private final JobIngestionService  ingestionService;
    private final ScanLogService       scanLogService;

    public JobIngestionScheduler(JobCollectorRegistry registry,
                                 JobIngestionService ingestionService,
                                 ScanLogService scanLogService) {
        this.registry         = registry;
        this.ingestionService = ingestionService;
        this.scanLogService   = scanLogService;
    }

    // Runs once 10 s after startup, then every 6 hours
    @Scheduled(initialDelayString = "10000", fixedDelayString = "21600000")
    public void run() {
        log.info("Starting job collection cycle...");
        try {
            JobCollectorRegistry.CollectResult result = registry.collectAll("SCHEDULER");
            if (result == null) {
                log.info("Scheduled scan skipped — a scan is already in progress");
                return;
            }
            JobIngestionService.IngestResult ingest = ingestionService.ingest(result.jobs());
            scanLogService.finalize(result.scanLogId(), Instant.now(),
                    result.perCompany(), ingest.ingestedPerCompany(), ingest.totalCreated());
        } catch (Exception e) {
            log.error("Job collection cycle failed", e);
        }
    }
}
