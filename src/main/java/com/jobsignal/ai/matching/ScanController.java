package com.jobsignal.ai.matching;

import com.jobsignal.ai.job.JobStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ScanController {

    private final JobMatchRepository jobMatchRepo;

    public ScanController(JobMatchRepository jobMatchRepo) {
        this.jobMatchRepo = jobMatchRepo;
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
}
