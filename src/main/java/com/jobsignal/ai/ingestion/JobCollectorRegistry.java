package com.jobsignal.ai.ingestion;

import com.jobsignal.ai.company.Company;
import com.jobsignal.ai.company.CompanyRepository;
import com.jobsignal.ai.company.SourceType;
import com.jobsignal.ai.ingestion.collector.CompanyCollector;
import com.jobsignal.ai.scanlog.ScanLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Loads all active companies from the database and dispatches each one to the
 * correct {@link CompanyCollector} implementation based on the company's
 * configured {@code source_type}.
 *
 * All {@link CompanyCollector} beans are auto-discovered from the Spring context
 * and indexed by {@link SourceType} at startup — no manual wiring needed.
 *
 * Companies are collected in parallel using a virtual-thread executor so that
 * slow HTTP calls (LinkedIn scraping, Greenhouse API) don't block each other.
 *
 * To support a new company: add a row to the companies table with the correct
 * source_type (and board_slug if applicable). No code change required.
 *
 * To support a new source type: add a new {@link CompanyCollector} @Component
 * and a matching {@link SourceType} enum value.
 */
@Service
public class JobCollectorRegistry {

    private static final Logger log = LoggerFactory.getLogger(JobCollectorRegistry.class);

    private final CompanyRepository                 companyRepo;
    private final Map<SourceType, CompanyCollector> collectors;
    private final ScanLogService                    scanLogService;

    /** Guards against concurrent collection cycles (scheduler + manual). */
    private final AtomicBoolean running = new AtomicBoolean(false);

    public JobCollectorRegistry(CompanyRepository companyRepo,
                                List<CompanyCollector> collectorList,
                                ScanLogService scanLogService) {
        this.companyRepo    = companyRepo;
        this.scanLogService = scanLogService;

        Map<SourceType, CompanyCollector> map = new EnumMap<>(SourceType.class);
        for (CompanyCollector c : collectorList) {
            map.put(c.sourceType(), c);
            log.info("Registered collector: {} → {}", c.sourceType(), c.getClass().getSimpleName());
        }
        this.collectors = map;
    }

    /**
     * Result of a collection cycle: the aggregated raw jobs plus the scan log ID
     * needed by the caller to finalize the log after ingestion.
     */
    public record CollectResult(Long scanLogId, List<RawJob> jobs, Map<String, Integer> perCompany) {}

    /**
     * Opens a scan log, collects raw jobs from all active companies in parallel,
     * and returns both the jobs and the open scanLogId for the caller to finalize.
     *
     * Each company runs in its own virtual thread so slow HTTP collectors (e.g.
     * LinkedIn) do not block faster ones (e.g. Greenhouse).
     *
     * Returns {@code null} if a scan is already in progress — callers must handle null.
     *
     * @param triggeredBy    "SCHEDULER" or "MANUAL"
     * @param onCompanyDone  optional callback invoked immediately when each company finishes —
     *                       receives (companyName, fetchedCount). Pass {@code null} to skip.
     */
    public CollectResult collectAll(String triggeredBy, BiConsumer<String, Integer> onCompanyDone) {
        if (!running.compareAndSet(false, true)) {
            log.warn("Scan already in progress — skipping '{}' trigger", triggeredBy);
            return null;
        }
        try {
            Instant startedAt = Instant.now();
            Long scanLogId = scanLogService.open(startedAt, triggeredBy);

            List<Company> companies = companyRepo.findAllByActiveTrue();

            // Collect results into thread-safe structures
            List<RawJob> all                     = new CopyOnWriteArrayList<>();
            Map<String, Integer> perCompany      = new ConcurrentHashMap<>();

            // One virtual thread per company — blocked I/O threads are cheap
            try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> futures = new ArrayList<>(companies.size());

                for (Company company : companies) {
                    futures.add(exec.submit(() -> {
                        SourceType sourceType = company.getSourceType();
                        if (sourceType == null) {
                            log.debug("'{}' has no source_type — skipping", company.getName());
                            perCompany.put(company.getName(), 0);
                            return;
                        }

                        CompanyCollector collector = collectors.get(sourceType);
                        if (collector == null) {
                            log.warn("No collector for source_type '{}' (company: '{}') — skipping",
                                    sourceType, company.getName());
                            perCompany.put(company.getName(), 0);
                            return;
                        }

                        List<RawJob> fetched = collector.collect(company);
                        perCompany.put(company.getName(), fetched.size());
                        all.addAll(fetched);
                        if (onCompanyDone != null) {
                            onCompanyDone.accept(company.getName(), fetched.size());
                        }
                    }));
                }

                // Wait for all companies to finish
                for (Future<?> f : futures) {
                    try {
                        f.get();
                    } catch (Exception e) {
                        log.error("Company collection task failed", e);
                    }
                }
            }

            // Rebuild perCompany as insertion-ordered LinkedHashMap sorted by company name
            Map<String, Integer> ordered = new LinkedHashMap<>();
            companies.stream()
                     .map(Company::getName)
                     .forEach(name -> ordered.put(name, perCompany.getOrDefault(name, 0)));

            log.info("Collection complete — {} companies, {} raw jobs fetched", companies.size(), all.size());
            return new CollectResult(scanLogId, new ArrayList<>(all), ordered);
        } finally {
            running.set(false);
        }
    }

    /**
     * Convenience overload — no per-company callback (used by scheduler).
     */
    public CollectResult collectAll(String triggeredBy) {
        return collectAll(triggeredBy, null);
    }

    /** Whether a collection cycle is currently executing. */
    public boolean isRunning() {
        return running.get();
    }
}
