package com.jobsignal.ai.application;

import com.jobsignal.ai.job.Job;
import com.jobsignal.ai.profile.ResumeProfile;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_profile_id", nullable = false)
    private ResumeProfile resumeProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApplicationStatus status = ApplicationStatus.SAVED;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "response_at")
    private Instant responseAt;

    @Column(name = "interview_date")
    private Instant interviewDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }

    public Job getJob()                             { return job; }
    public void setJob(Job job)                     { this.job = job; }

    public ResumeProfile getResumeProfile()                         { return resumeProfile; }
    public void setResumeProfile(ResumeProfile resumeProfile)       { this.resumeProfile = resumeProfile; }

    public ApplicationStatus getStatus()                            { return status; }
    public void setStatus(ApplicationStatus status)                 { this.status = status; }

    public Instant getAppliedAt()                   { return appliedAt; }
    public void setAppliedAt(Instant appliedAt)     { this.appliedAt = appliedAt; }

    public Instant getResponseAt()                  { return responseAt; }
    public void setResponseAt(Instant responseAt)   { this.responseAt = responseAt; }

    public Instant getInterviewDate()               { return interviewDate; }
    public void setInterviewDate(Instant interviewDate) { this.interviewDate = interviewDate; }

    public String getNotes()                        { return notes; }
    public void setNotes(String notes)              { this.notes = notes; }

    public Instant getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(Instant createdAt)     { this.createdAt = createdAt; }

    public Instant getUpdatedAt()                   { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt)     { this.updatedAt = updatedAt; }
}
