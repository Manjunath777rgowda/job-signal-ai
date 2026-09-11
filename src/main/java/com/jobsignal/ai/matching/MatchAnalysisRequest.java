package com.jobsignal.ai.matching;

public class MatchAnalysisRequest {

    private String jobTitle;
    private String jobDescription;

    public String getJobTitle()                      { return jobTitle; }
    public void setJobTitle(String jobTitle)         { this.jobTitle = jobTitle; }

    public String getJobDescription()                        { return jobDescription; }
    public void setJobDescription(String jobDescription)     { this.jobDescription = jobDescription; }
}
