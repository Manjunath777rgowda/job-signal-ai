package com.jobsignal.ai.scanlog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScanLogEntryRepository extends JpaRepository<ScanLogEntry, Long> {

    @Query("SELECT e FROM ScanLogEntry e WHERE e.scanLog.id = :scanLogId ORDER BY e.fetched DESC")
    List<ScanLogEntry> findByScanLogIdOrderByFetchedDesc(Long scanLogId);
}
