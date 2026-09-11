package com.jobsignal.ai.scanlog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScanLogRepository extends JpaRepository<ScanLog, Long> {

    /** Returns the most recently started scan. */
    @Query("SELECT s FROM ScanLog s ORDER BY s.startedAt DESC LIMIT 1")
    Optional<ScanLog> findLatest();

    /** Returns all scans, newest first. */
    @Query("SELECT s FROM ScanLog s ORDER BY s.startedAt DESC")
    List<ScanLog> findAllOrderByStartedAtDesc();
}
