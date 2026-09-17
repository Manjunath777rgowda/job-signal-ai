package com.jobsignal.ai.matching;

import com.jobsignal.ai.job.Job;
import com.jobsignal.ai.job.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@RestController
@RequestMapping("/api/v1")
public class ScanController {

    private static final Logger log = LoggerFactory.getLogger(ScanController.class);

    private final JobMatchRepository jobMatchRepo;
    private final com.jobsignal.ai.job.JobRepository jobRepo;
    private final MatchingService    matchingService;

    public ScanController(JobMatchRepository jobMatchRepo,
                          com.jobsignal.ai.job.JobRepository jobRepo,
                          MatchingService matchingService) {
        this.jobMatchRepo    = jobMatchRepo;
        this.jobRepo         = jobRepo;
        this.matchingService = matchingService;
    }

    /**
     * Returns all scored jobs, with optional server-side sort and location filter.
     *
     * @param sort     "score" (default) or "posted"
     * @param location optional substring match against the job's location field (case-insensitive)
     */
    @GetMapping("/scan")
    public ResponseEntity<List<JobScanResult>> scan(
            @RequestParam(defaultValue = "score")  String sort,
            @RequestParam(required = false)        String location) {

        List<JobMatch> matches = "posted".equalsIgnoreCase(sort)
                ? jobMatchRepo.findAllOrderByPostedDesc()
                : jobMatchRepo.findAllOrderByScoreDesc();

        List<JobScanResult> results = matches.stream()
                // Only exclude permanently-hidden statuses; APPLIED and ARCHIVED are shown to the UI
                .filter(m -> m.getJob().getStatus() != JobStatus.CLOSED
                          && m.getJob().getStatus() != JobStatus.DUPLICATE)
                .filter(m -> {
                    if (location == null || location.isBlank()) return true;
                    String loc = m.getJob().getLocation();
                    return loc != null && loc.toLowerCase().contains(location.toLowerCase());
                })
                .map(JobScanResult::new)
                .toList();

        return ResponseEntity.ok(results);
    }

    /**
     * Re-evaluates all existing job matches using the current matching rules / resume profile.
     * Preserves job status (ACTIVE, APPLIED, ARCHIVED) and only recalculates scores, fit,
     * recommendations, strengths, and gaps.
     */
    @PostMapping("/scan/re-evaluate")
    @Transactional
    public ResponseEntity<Map<String, Object>> reEvaluate() {
        List<JobMatch> matches = jobMatchRepo.findAll();
        int evaluatedCount = 0;

        for (JobMatch match : matches) {
            Job job = match.getJob();
            if (job == null) continue;

            MatchAnalysisRequest req = new MatchAnalysisRequest();
            req.setJobTitle(job.getTitle());
            req.setJobDescription(job.getDescription() != null ? job.getDescription() : "");

            MatchAnalysisResponse analysis = matchingService.analyze(req);

            match.setSkillScore(analysis.getSkillScore());
            match.setExperienceScore(analysis.getExperienceScore());
            match.setResponsibilityScore(analysis.getResponsibilityScore());
            match.setDomainScore(analysis.getDomainScore());
            match.setLocationScore(analysis.getLocationScore());
            match.setOtherScore(analysis.getOtherScore());
            match.setOverallScore(analysis.getOverallScore());
            match.setInterviewFit(analysis.getInterviewFit());
            match.setRecommendation(analysis.getRecommendation());
            match.setStrengths(joinTags(analysis.getStrengths()));
            match.setGaps(joinTags(analysis.getGaps()));

            jobMatchRepo.save(match);
            evaluatedCount++;
        }

        log.info("Re-evaluated {} job matches successfully", evaluatedCount);
        return ResponseEntity.ok(Map.of(
                "evaluated", evaluatedCount,
                "message", "Re-evaluation completed successfully"
        ));
    }

    /**
     * Cleans up all jobs and job matches that are NOT APPLIED or ARCHIVED (i.e. ACTIVE/unmarked).
     * Preserves user-tracked jobs (APPLIED, ARCHIVED).
     */
    @PostMapping("/scan/cleanup")
    @Transactional
    public ResponseEntity<Map<String, Object>> cleanup() {
        List<JobMatch> matches = jobMatchRepo.findAll();
        int deletedMatches = 0;
        int deletedJobs = 0;

        for (JobMatch match : matches) {
            Job job = match.getJob();
            if (job != null && job.getStatus() != JobStatus.APPLIED && job.getStatus() != JobStatus.ARCHIVED) {
                jobMatchRepo.delete(match);
                deletedMatches++;
            }
        }

        List<Job> allJobs = jobRepo.findAll();
        for (Job job : allJobs) {
            if (job.getStatus() != JobStatus.APPLIED && job.getStatus() != JobStatus.ARCHIVED) {
                jobRepo.delete(job);
                deletedJobs++;
            }
        }

        log.info("Cleaned up {} unapplied/unarchived jobs and {} matches", deletedJobs, deletedMatches);
        return ResponseEntity.ok(Map.of(
                "deletedJobs", deletedJobs,
                "deletedMatches", deletedMatches,
                "message", "Cleanup completed successfully"
        ));
    }

    private String joinTags(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringJoiner sj = new StringJoiner(", ");
        items.forEach(sj::add);
        return sj.toString();
    }
}
