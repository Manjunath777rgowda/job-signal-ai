package com.jobsignal.ai.scanlog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Scan log endpoints:
 *   GET /api/v1/scan/log          — latest scan (used on page load, backward-compat)
 *   GET /api/v1/scan/logs         — all scans, newest first, summary only (no entries)
 *   GET /api/v1/scan/log/{id}     — one specific scan with full per-company entries
 */
@RestController
@RequestMapping("/api/v1/scan")
public class ScanLogController {

    private final ScanLogService scanLogService;

    public ScanLogController(ScanLogService scanLogService) {
        this.scanLogService = scanLogService;
    }

    /** Latest scan — kept for backward compatibility with page-load call. */
    @GetMapping("/log")
    public ResponseEntity<Map<String, Object>> getLatest() {
        Optional<ScanLog> opt = scanLogService.getLatest();
        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of("status", "no_scan_yet"));
        }
        return ResponseEntity.ok(toDetailMap(opt.get()));
    }

    /** All scans, newest first — summary rows only (no entries). */
    @GetMapping("/logs")
    public ResponseEntity<List<Map<String, Object>>> listAll() {
        List<Map<String, Object>> rows = scanLogService.findAll().stream()
                .map(sl -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("scanId",        sl.getId());
                    m.put("startedAt",     sl.getStartedAt());
                    m.put("finishedAt",    sl.getFinishedAt());
                    m.put("totalFetched",  sl.getTotalFetched());
                    m.put("totalIngested", sl.getTotalIngested());
                    m.put("triggeredBy",   sl.getTriggeredBy());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(rows);
    }

    /** Single scan with full per-company breakdown. */
    @GetMapping("/log/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return scanLogService.findById(id)
                .<ResponseEntity<Map<String, Object>>>map(sl -> ResponseEntity.ok(toDetailMap(sl)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private Map<String, Object> toDetailMap(ScanLog sl) {
        List<Map<String, Object>> entries = sl.getEntries().stream()
                .sorted((a, b) -> Integer.compare(b.getIngested(), a.getIngested()))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("company",  e.getCompany());
                    m.put("fetched",  e.getFetched());
                    m.put("ingested", e.getIngested());
                    return m;
                })
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("scanId",        sl.getId());
        body.put("startedAt",     sl.getStartedAt());
        body.put("finishedAt",    sl.getFinishedAt());
        body.put("totalFetched",  sl.getTotalFetched());
        body.put("totalIngested", sl.getTotalIngested());
        body.put("triggeredBy",   sl.getTriggeredBy());
        body.put("entries",       entries);
        return body;
    }
}
