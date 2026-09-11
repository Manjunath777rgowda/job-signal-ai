package com.jobsignal.ai.ingestion;

/**
 * Normalised job posting fetched from any source.
 * All fields except externalId, title and description may be null.
 */
public class RawJob {

    private final String companyName;
    private final String externalId;
    private final String title;
    private final String description;
    private final String location;
    private final String jobUrl;

    public RawJob(String companyName, String externalId, String title,
                  String description, String location, String jobUrl) {
        this.companyName = companyName;
        this.externalId  = externalId;
        this.title       = title;
        this.description = description;
        this.location    = location;
        this.jobUrl      = jobUrl;
    }

    public String getCompanyName() { return companyName; }
    public String getExternalId()  { return externalId; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }
    public String getLocation()    { return location; }
    public String getJobUrl()      { return jobUrl; }
}
