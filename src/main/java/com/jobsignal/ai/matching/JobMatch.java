package com.jobsignal.ai.matching;

import com.jobsignal.ai.job.Job;
import com.jobsignal.ai.profile.ResumeProfile;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_matches")
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_profile_id", nullable = false)
    private ResumeProfile resumeProfile;

    // Per-dimension scores (0–100 each)
    // Weights: Skills 30%, Experience 20%, Responsibilities 20%, Domain 20%, Location 5%, Other 5%
    @Column(name = "skill_score", nullable = false)
    private Integer skillScore;

    @Column(name = "experience_score", nullable = false)
    private Integer experienceScore;

    @Column(name = "responsibility_score", nullable = false)
    private Integer responsibilityScore;

    @Column(name = "domain_score", nullable = false)
    private Integer domainScore;

    @Column(name = "location_score", nullable = false)
    private Integer locationScore;

    @Column(name = "other_score", nullable = false)
    private Integer otherScore;

    // Composite weighted score
    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "interview_fit", nullable = false, length = 20)
    private String interviewFit;

    @Column(name = "recommendation", nullable = false, length = 20)
    private String recommendation;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "gaps", columnDefinition = "TEXT")
    private String gaps;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public Job getJob()                         { return job; }
    public void setJob(Job job)                 { this.job = job; }

    public ResumeProfile getResumeProfile()                         { return resumeProfile; }
    public void setResumeProfile(ResumeProfile resumeProfile)       { this.resumeProfile = resumeProfile; }

    public Integer getSkillScore()                                  { return skillScore; }
    public void setSkillScore(Integer skillScore)                   { this.skillScore = skillScore; }

    public Integer getExperienceScore()                             { return experienceScore; }
    public void setExperienceScore(Integer experienceScore)         { this.experienceScore = experienceScore; }

    public Integer getResponsibilityScore()                         { return responsibilityScore; }
    public void setResponsibilityScore(Integer responsibilityScore) { this.responsibilityScore = responsibilityScore; }

    public Integer getDomainScore()                                 { return domainScore; }
    public void setDomainScore(Integer domainScore)                 { this.domainScore = domainScore; }

    public Integer getLocationScore()                               { return locationScore; }
    public void setLocationScore(Integer locationScore)             { this.locationScore = locationScore; }

    public Integer getOtherScore()                                  { return otherScore; }
    public void setOtherScore(Integer otherScore)                   { this.otherScore = otherScore; }

    public Integer getOverallScore()                                { return overallScore; }
    public void setOverallScore(Integer overallScore)               { this.overallScore = overallScore; }

    public String getInterviewFit()                                 { return interviewFit; }
    public void setInterviewFit(String interviewFit)                { this.interviewFit = interviewFit; }

    public String getRecommendation()                               { return recommendation; }
    public void setRecommendation(String recommendation)            { this.recommendation = recommendation; }

    public String getStrengths()                                    { return strengths; }
    public void setStrengths(String strengths)                      { this.strengths = strengths; }

    public String getGaps()                                         { return gaps; }
    public void setGaps(String gaps)                                { this.gaps = gaps; }

    public Instant getCreatedAt()                                   { return createdAt; }
    public void setCreatedAt(Instant createdAt)                     { this.createdAt = createdAt; }
}
