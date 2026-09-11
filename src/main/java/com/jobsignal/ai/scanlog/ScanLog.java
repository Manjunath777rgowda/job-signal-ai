package com.jobsignal.ai.scanlog;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scan_logs")
public class ScanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "total_fetched", nullable = false)
    private int totalFetched = 0;

    @Column(name = "total_ingested", nullable = false)
    private int totalIngested = 0;

    @Column(name = "triggered_by", nullable = false, length = 20)
    private String triggeredBy = "SCHEDULER";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "scanLog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ScanLogEntry> entries = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // ── getters / setters ──────────────────────────────────────────────────────

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }

    public Instant getStartedAt()                   { return startedAt; }
    public void setStartedAt(Instant startedAt)     { this.startedAt = startedAt; }

    public Instant getFinishedAt()                  { return finishedAt; }
    public void setFinishedAt(Instant finishedAt)   { this.finishedAt = finishedAt; }

    public int getTotalFetched()                    { return totalFetched; }
    public void setTotalFetched(int totalFetched)   { this.totalFetched = totalFetched; }

    public int getTotalIngested()                   { return totalIngested; }
    public void setTotalIngested(int totalIngested) { this.totalIngested = totalIngested; }

    public String getTriggeredBy()                      { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy)      { this.triggeredBy = triggeredBy; }

    public Instant getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(Instant createdAt)     { this.createdAt = createdAt; }

    public List<ScanLogEntry> getEntries()          { return entries; }
    public void setEntries(List<ScanLogEntry> e)    { this.entries = e; }
}
