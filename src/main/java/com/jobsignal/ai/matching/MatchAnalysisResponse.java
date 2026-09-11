package com.jobsignal.ai.matching;

import java.util.List;

public class MatchAnalysisResponse {

    private final int overallScore;
    private final String interviewFit;
    private final String recommendation;

    // Per-dimension scores (0–100 each)
    // Weights: Skills 30%, Experience 20%, Responsibilities 20%, Domain 20%, Location 5%, Other 5%
    private final int skillScore;
    private final int experienceScore;
    private final int responsibilityScore;
    private final int domainScore;
    private final int locationScore;
    private final int otherScore;

    private final List<String> strengths;
    private final List<String> gaps;

    public MatchAnalysisResponse(
            int overallScore,
            String interviewFit,
            String recommendation,
            int skillScore,
            int experienceScore,
            int responsibilityScore,
            int domainScore,
            int locationScore,
            int otherScore,
            List<String> strengths,
            List<String> gaps) {
        this.overallScore         = overallScore;
        this.interviewFit         = interviewFit;
        this.recommendation       = recommendation;
        this.skillScore           = skillScore;
        this.experienceScore      = experienceScore;
        this.responsibilityScore  = responsibilityScore;
        this.domainScore          = domainScore;
        this.locationScore        = locationScore;
        this.otherScore           = otherScore;
        this.strengths            = strengths;
        this.gaps                 = gaps;
    }

    public int getOverallScore()          { return overallScore; }
    public String getInterviewFit()       { return interviewFit; }
    public String getRecommendation()     { return recommendation; }
    public int getSkillScore()            { return skillScore; }
    public int getExperienceScore()       { return experienceScore; }
    public int getResponsibilityScore()   { return responsibilityScore; }
    public int getDomainScore()           { return domainScore; }
    public int getLocationScore()         { return locationScore; }
    public int getOtherScore()            { return otherScore; }
    public List<String> getStrengths()    { return strengths; }
    public List<String> getGaps()         { return gaps; }
}
