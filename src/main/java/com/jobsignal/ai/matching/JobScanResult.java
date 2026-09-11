package com.jobsignal.ai.matching;

import com.jobsignal.ai.job.Job;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * View model for a scored job — used by the dashboard API.
 */
public class JobScanResult {

    private final Long   jobId;
    private final String companyName;
    private final String jobTitle;
    private final String jobUrl;
    private final String location;
    private final String freshness;
    private final String firstSeenAt;   // ISO-8601 for client-side sort
    private final String jobStatus;     // ACTIVE | ARCHIVED | APPLIED | …
    private final int    overallScore;
    private final String interviewFit;
    private final String recommendation;
    private final int    skillScore;
    private final int    experienceScore;
    private final int    responsibilityScore;
    private final int    domainScore;
    private final int    locationScore;
    private final int    otherScore;
    private final List<String> strengths;
    private final List<String> gaps;

    /** Constructor from a persisted JobMatch (real DB data). */
    public JobScanResult(JobMatch match) {
        Job job = match.getJob();
        this.jobId               = job.getId();
        this.companyName         = job.getCompany().getName();
        this.jobTitle            = job.getTitle();
        this.jobUrl              = job.getJobUrl();
        this.location            = job.getLocation();
        this.freshness           = freshness(job.getFirstSeenAt());
        this.firstSeenAt         = job.getFirstSeenAt() != null ? job.getFirstSeenAt().toString() : null;
        this.jobStatus           = job.getStatus() != null ? job.getStatus().name() : "ACTIVE";
        this.overallScore        = match.getOverallScore();
        this.interviewFit        = match.getInterviewFit();
        this.recommendation      = match.getRecommendation();
        this.skillScore          = match.getSkillScore();
        this.experienceScore     = match.getExperienceScore();
        this.responsibilityScore = match.getResponsibilityScore();
        this.domainScore         = match.getDomainScore();
        this.locationScore       = match.getLocationScore();
        this.otherScore          = match.getOtherScore();
        this.strengths           = splitTags(match.getStrengths());
        this.gaps                = splitTags(match.getGaps());
    }

    /** Constructor from an in-memory analysis (no DB). */
    public JobScanResult(String jobTitle, MatchAnalysisResponse r) {
        this.jobId               = null;
        this.companyName         = null;
        this.jobTitle            = jobTitle;
        this.jobUrl              = null;
        this.location            = null;
        this.freshness           = "HOT";
        this.firstSeenAt         = null;
        this.jobStatus           = "ACTIVE";
        this.overallScore        = r.getOverallScore();
        this.interviewFit        = r.getInterviewFit();
        this.recommendation      = r.getRecommendation();
        this.skillScore          = r.getSkillScore();
        this.experienceScore     = r.getExperienceScore();
        this.responsibilityScore = r.getResponsibilityScore();
        this.domainScore         = r.getDomainScore();
        this.locationScore       = r.getLocationScore();
        this.otherScore          = r.getOtherScore();
        this.strengths           = r.getStrengths();
        this.gaps                = r.getGaps();
    }

    private static String freshness(Instant firstSeen) {
        if (firstSeen == null) return "UNKNOWN";
        long hours = Duration.between(firstSeen, Instant.now()).toHours();
        if (hours < 24)   return "HOT";
        if (hours < 72)   return "NEW";
        if (hours < 168)  return "ACTIVE";
        if (hours < 336)  return "OLD";
        return "LOW_PRIORITY";
    }

    private static List<String> splitTags(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public Long   getJobId()               { return jobId; }
    public String getCompanyName()         { return companyName; }
    public String getJobTitle()            { return jobTitle; }
    public String getJobUrl()              { return jobUrl; }
    public String getLocation()            { return location; }
    public String getFreshness()           { return freshness; }
    public String getFirstSeenAt()         { return firstSeenAt; }
    public String getJobStatus()           { return jobStatus; }
    public int    getOverallScore()        { return overallScore; }
    public String getInterviewFit()        { return interviewFit; }
    public String getRecommendation()      { return recommendation; }
    public int    getSkillScore()          { return skillScore; }
    public int    getExperienceScore()     { return experienceScore; }
    public int    getResponsibilityScore() { return responsibilityScore; }
    public int    getDomainScore()         { return domainScore; }
    public int    getLocationScore()       { return locationScore; }
    public int    getOtherScore()          { return otherScore; }
    public List<String> getStrengths()     { return strengths; }
    public List<String> getGaps()          { return gaps; }
}
