package com.jobsignal.ai.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Job management endpoints.
 *
 * PATCH /api/v1/jobs/{id}/status — sets status to ACTIVE or ARCHIVED.
 *
 * Hard-delete is intentionally not supported: removing the DB row would lose the
 * external_job_id record, causing the same job to be re-ingested on the next scan.
 * Archiving hides the job from the dashboard while preserving the deduplication key.
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    private final JobRepository jobRepo;

    public JobController(JobRepository jobRepo) {
        this.jobRepo = jobRepo;
    }

    /**
     * Sets the job's status field.
     * Body: { "status": "ARCHIVED" | "ACTIVE" }
     */
    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<Map<String, String>> setStatus(
            @PathVariable Long id,
            @RequestBody  Map<String, String> body) {

        return jobRepo.findById(id).map(job -> {
            String raw = body.getOrDefault("status", "").toUpperCase();
            JobStatus newStatus;
            try {
                newStatus = JobStatus.valueOf(raw);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .<Map<String, String>>body(Map.of("error", "Unknown status: " + raw));
            }
            job.setStatus(newStatus);
            jobRepo.save(job);
            log.info("Job {} status set to {}", id, newStatus);
            return ResponseEntity.ok(Map.of("status", newStatus.name(), "id", String.valueOf(id)));
        }).orElse(ResponseEntity.notFound().build());
    }
}
