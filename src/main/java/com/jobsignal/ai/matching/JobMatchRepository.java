package com.jobsignal.ai.matching;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {

    /** All matches sorted by overall score descending. */
    @Query("SELECT m FROM JobMatch m JOIN FETCH m.job j JOIN FETCH j.company ORDER BY m.overallScore DESC")
    List<JobMatch> findAllOrderByScoreDesc();

    /** All matches sorted by first_seen_at descending (newest first). */
    @Query("SELECT m FROM JobMatch m JOIN FETCH m.job j JOIN FETCH j.company ORDER BY j.firstSeenAt DESC NULLS LAST")
    List<JobMatch> findAllOrderByPostedDesc();

    boolean existsByJobId(Long jobId);
}
