package com.jobsignal.ai.profile;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "resume_profiles")
public class ResumeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, length = 512)
    private String headline;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String experience;

    @Column(columnDefinition = "TEXT")
    private String certifications;

    @Column(name = "years_experience", nullable = false)
    private int yearsExperience;

    @Column(name = "target_roles", columnDefinition = "TEXT")
    private String targetRoles;

    @Column(length = 255)
    private String location;

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

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public String getFullName()                 { return fullName; }
    public void setFullName(String fullName)    { this.fullName = fullName; }

    public String getHeadline()                 { return headline; }
    public void setHeadline(String headline)    { this.headline = headline; }

    public String getSummary()                  { return summary; }
    public void setSummary(String summary)      { this.summary = summary; }

    public String getSkills()                   { return skills; }
    public void setSkills(String skills)        { this.skills = skills; }

    public String getExperience()               { return experience; }
    public void setExperience(String experience){ this.experience = experience; }

    public String getCertifications()                       { return certifications; }
    public void setCertifications(String certifications)    { this.certifications = certifications; }

    public int getYearsExperience()                         { return yearsExperience; }
    public void setYearsExperience(int yearsExperience)     { this.yearsExperience = yearsExperience; }

    public String getTargetRoles()                          { return targetRoles; }
    public void setTargetRoles(String targetRoles)          { this.targetRoles = targetRoles; }

    public String getLocation()                             { return location; }
    public void setLocation(String location)                { this.location = location; }

    public Instant getCreatedAt()               { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt()               { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
