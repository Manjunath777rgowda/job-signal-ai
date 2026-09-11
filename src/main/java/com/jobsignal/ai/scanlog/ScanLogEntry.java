package com.jobsignal.ai.scanlog;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "scan_log_entries")
public class ScanLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_log_id", nullable = false)
    private ScanLog scanLog;

    @Column(nullable = false, length = 255)
    private String company;

    @Column(nullable = false)
    private int fetched = 0;

    @Column(nullable = false)
    private int ingested = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // ── getters / setters ──────────────────────────────────────────────────────

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public ScanLog getScanLog()                     { return scanLog; }
    public void setScanLog(ScanLog scanLog)         { this.scanLog = scanLog; }

    public String getCompany()                  { return company; }
    public void setCompany(String company)      { this.company = company; }

    public int getFetched()                     { return fetched; }
    public void setFetched(int fetched)         { this.fetched = fetched; }

    public int getIngested()                    { return ingested; }
    public void setIngested(int ingested)       { this.ingested = ingested; }

    public Instant getCreatedAt()               { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
