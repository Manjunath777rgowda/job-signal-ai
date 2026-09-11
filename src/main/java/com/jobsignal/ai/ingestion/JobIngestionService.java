package com.jobsignal.ai.ingestion;

import com.jobsignal.ai.company.Company;
import com.jobsignal.ai.company.CompanyRepository;
import com.jobsignal.ai.job.Job;
import com.jobsignal.ai.job.JobRepository;
import com.jobsignal.ai.matching.*;
import com.jobsignal.ai.profile.ResumeProfile;
import com.jobsignal.ai.profile.ResumeProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Persists collected raw jobs into the database and runs match analysis
 * against the default resume profile. Skips jobs already seen (deduplication
 * by company + externalJobId). Updates lastSeenAt for existing jobs.
 */
@Service
public class JobIngestionService {

    private static final Logger log = LoggerFactory.getLogger(JobIngestionService.class);

    private final CompanyRepository     companyRepo;
    private final JobRepository         jobRepo;
    private final JobMatchRepository    jobMatchRepo;
    private final ResumeProfileRepository profileRepo;
    private final MatchingService       matchingService;

    public JobIngestionService(CompanyRepository companyRepo,
                               JobRepository jobRepo,
                               JobMatchRepository jobMatchRepo,
                               ResumeProfileRepository profileRepo,
                               MatchingService matchingService) {
        this.companyRepo     = companyRepo;
        this.jobRepo         = jobRepo;
        this.jobMatchRepo    = jobMatchRepo;
        this.profileRepo     = profileRepo;
        this.matchingService = matchingService;
    }

    /**
     * Result of an ingestion run: total new jobs created and a per-company
     * breakdown of how many were inserted (duplicates excluded).
     */
    public record IngestResult(int totalCreated, Map<String, Integer> ingestedPerCompany) {}

    /**
     * Persists new jobs and their match analysis. Skips duplicates.
     *
     * @return {@link IngestResult} with the total created count and per-company ingested map
     */
    @Transactional
    public IngestResult ingest(List<RawJob> rawJobs) {
        ResumeProfile profile = resolveDefaultProfile();
        if (profile == null) {
            log.warn("No ResumeProfile found — skipping ingestion. Seed one first.");
            return new IngestResult(0, Map.of());
        }

        int created = 0, updated = 0, skipped = 0;
        Map<String, Integer> ingestedPerCompany = new LinkedHashMap<>();

        for (RawJob raw : rawJobs) {
            if (raw.getExternalId() == null || raw.getExternalId().isBlank()
                    || raw.getTitle() == null || raw.getTitle().isBlank()) {
                skipped++;
                continue;
            }

            Company company = resolveCompany(raw.getCompanyName());
            if (company == null) {
                skipped++;
                continue;
            }

            // Deduplicate — job already exists; just refresh its lastSeenAt
            if (jobRepo.existsByCompanyIdAndExternalJobId(company.getId(), raw.getExternalId())) {
                jobRepo.touchLastSeenAt(company.getId(), raw.getExternalId(), Instant.now());
                updated++;
                continue;
            }

            // Persist job
            Job job = new Job();
            job.setCompany(company);
            job.setExternalJobId(raw.getExternalId());
            job.setTitle(raw.getTitle());
            job.setDescription(raw.getDescription() != null ? raw.getDescription() : "");
            job.setLocation(raw.getLocation());
            job.setJobUrl(raw.getJobUrl());
            job.setFirstSeenAt(Instant.now());
            job.setLastSeenAt(Instant.now());
            job = jobRepo.save(job);

            // Run match analysis and persist
            MatchAnalysisRequest req = new MatchAnalysisRequest();
            req.setJobTitle(raw.getTitle());
            req.setJobDescription(job.getDescription());
            MatchAnalysisResponse analysis = matchingService.analyze(req);

            JobMatch match = new JobMatch();
            match.setJob(job);
            match.setResumeProfile(profile);
            match.setSkillScore(analysis.getSkillScore());
            match.setExperienceScore(analysis.getExperienceScore());
            match.setResponsibilityScore(analysis.getResponsibilityScore());
            match.setDomainScore(analysis.getDomainScore());
            match.setLocationScore(analysis.getLocationScore());
            match.setOtherScore(analysis.getOtherScore());
            match.setOverallScore(analysis.getOverallScore());
            match.setInterviewFit(analysis.getInterviewFit());
            match.setRecommendation(analysis.getRecommendation());
            match.setStrengths(join(analysis.getStrengths()));
            match.setGaps(join(analysis.getGaps()));
            jobMatchRepo.save(match);

            ingestedPerCompany.merge(raw.getCompanyName(), 1, Integer::sum);
            created++;
        }

        log.info("Ingestion complete — created: {}, already seen: {}, skipped: {}", created, updated, skipped);
        return new IngestResult(created, ingestedPerCompany);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResumeProfile resolveDefaultProfile() {
        return profileRepo.findAll().stream().findFirst().orElse(null);
    }

    private Company resolveCompany(String name) {
        if (name == null) return null;
        Optional<Company> match = companyRepo.findByNameIgnoreCase(name);
        if (match.isPresent()) return match.get();
        log.debug("Company '{}' not found in DB — skipping", name);
        return null;
    }

    private String join(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringJoiner sj = new StringJoiner(", ");
        items.forEach(sj::add);
        return sj.toString();
    }
}
