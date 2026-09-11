package com.jobsignal.ai.scanlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persists scan audit logs to the database and exposes the latest scan for the API.
 * One {@link ScanLog} row is created when a scan starts; it is updated with final
 * counts when the scan finishes.
 */
@Service
public class ScanLogService {

    private static final Logger log = LoggerFactory.getLogger(ScanLogService.class);

    private final ScanLogRepository      scanLogRepo;
    private final ScanLogEntryRepository entryRepo;

    public ScanLogService(ScanLogRepository scanLogRepo, ScanLogEntryRepository entryRepo) {
        this.scanLogRepo = scanLogRepo;
        this.entryRepo   = entryRepo;
    }

    /**
     * Opens a new scan log row and returns its ID.
     *
     * @param startedAt   when the scan started
     * @param triggeredBy "SCHEDULER" or "MANUAL"
     * @return the persisted {@link ScanLog} ID
     */
    @Transactional
    public Long open(Instant startedAt, String triggeredBy) {
        ScanLog sl = new ScanLog();
        sl.setStartedAt(startedAt);
        sl.setTriggeredBy(triggeredBy);
        scanLogRepo.save(sl);
        log.debug("Opened scan log id={}", sl.getId());
        return sl.getId();
    }

    /**
     * Finalises an open scan log with per-company counts and ingestion totals.
     *
     * @param scanLogId          the ID returned by {@link #open}
     * @param finishedAt         when the scan finished
     * @param fetchedPerCompany  map of company name → raw jobs fetched from the web
     * @param ingestedPerCompany map of company name → new jobs inserted into DB (duplicates excluded)
     * @param totalIngested      total new jobs inserted across all companies
     */
    @Transactional
    public void finalize(Long scanLogId, Instant finishedAt,
                         Map<String, Integer> fetchedPerCompany,
                         Map<String, Integer> ingestedPerCompany,
                         int totalIngested) {
        Optional<ScanLog> opt = scanLogRepo.findById(scanLogId);
        if (opt.isEmpty()) {
            log.warn("ScanLog id={} not found — skipping finalize", scanLogId);
            return;
        }
        ScanLog sl = opt.get();

        // Persist per-company entries — fetched from web, ingested into DB
        int totalFetched = 0;
        for (Map.Entry<String, Integer> e : fetchedPerCompany.entrySet()) {
            ScanLogEntry entry = new ScanLogEntry();
            entry.setScanLog(sl);
            entry.setCompany(e.getKey());
            entry.setFetched(e.getValue());
            entry.setIngested(ingestedPerCompany.getOrDefault(e.getKey(), 0));
            entryRepo.save(entry);
            totalFetched += e.getValue();
        }

        sl.setFinishedAt(finishedAt);
        sl.setTotalFetched(totalFetched);
        sl.setTotalIngested(totalIngested);
        scanLogRepo.save(sl);

        log.info("Scan log id={} finalized — fetched: {}, ingested: {}", sl.getId(), totalFetched, totalIngested);
    }

    /** Returns the most recent scan log with its entries, or empty if none exists. */
    @Transactional(readOnly = true)
    public Optional<ScanLog> getLatest() {
        return scanLogRepo.findLatest().map(sl -> {
            sl.getEntries().size();
            return sl;
        });
    }

    /** Returns all scan logs newest-first, with entries eagerly loaded. */
    @Transactional(readOnly = true)
    public List<ScanLog> findAll() {
        return scanLogRepo.findAllOrderByStartedAtDesc().stream()
                .peek(sl -> sl.getEntries().size())
                .toList();
    }

    /** Returns a single scan log by id with entries, or empty. */
    @Transactional(readOnly = true)
    public Optional<ScanLog> findById(Long id) {
        return scanLogRepo.findById(id).map(sl -> {
            sl.getEntries().size();
            return sl;
        });
    }
}
