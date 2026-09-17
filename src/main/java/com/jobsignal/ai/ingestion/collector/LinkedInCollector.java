package com.jobsignal.ai.ingestion.collector;

import com.jobsignal.ai.company.Company;
import com.jobsignal.ai.company.SourceType;
import com.jobsignal.ai.ingestion.RawJob;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fetches jobs from the LinkedIn public job-search guest API.
 *
 * Used for companies whose native ATS APIs are unavailable:
 *   Google, Microsoft, Oracle, Meta, Adobe, Salesforce, Uber, Atlassian, Apple.
 *
 * Endpoint: https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search
 * Individual JD: https://www.linkedin.com/jobs-guest/jobs/api/jobPosting/{jobId}
 *
 * No API key required — this is the same HTML the LinkedIn guest job search page uses.
 * Rate-limit: sleeps 500 ms between individual JD fetches to be polite.
 */
@Component
public class LinkedInCollector extends CompanyCollector {

    private static final String SEARCH_URL =
            "https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search";
    private static final String JD_URL =
            "https://www.linkedin.com/jobs-guest/jobs/api/jobPosting/";

    // India geo-id on LinkedIn
    private static final String GEO_INDIA = "102713980";

    /** Mutex to serialize requests to LinkedIn across concurrent virtual threads. */
    private static final Object LINKEDIN_LOCK = new Object();

    @Override
    public SourceType sourceType() {
        return SourceType.LINKEDIN;
    }

    @Override
    public List<RawJob> collect(Company company) {
        String keywords = URLEncoder.encode(
                "Senior Software Engineer " + company.getName(), StandardCharsets.UTF_8);
        String url = SEARCH_URL
                + "?keywords=" + keywords
                + "&location=India"
                + "&geoId=" + GEO_INDIA
                + "&start=0";

        try {
            String html = getWithRetry(url);
            Document doc = Jsoup.parse(html);

            // Each job is a <li> containing a div with data-entity-urn
            Elements cards = doc.select("div[data-entity-urn]");
            if (cards.isEmpty()) {
                // fallback: li elements with job-search-card class
                cards = doc.select("li.job-search-card, div.job-search-card");
            }

            List<RawJob> results = new ArrayList<>();

            for (Element card : cards) {
                String urn = card.attr("data-entity-urn");   // urn:li:jobPosting:12345
                String jobId = urn.replaceAll(".*:(\\d+)$", "$1");
                if (jobId.equals(urn) || jobId.isBlank()) continue; // parse failed

                // Verify company name matches target to prevent keyword spillover
                Element compEl = card.selectFirst("h4.base-search-card__subtitle, .job-search-card__company-name");
                String cardCompany = compEl != null ? compEl.text().trim() : "";
                if (!matchesTargetCompany(cardCompany, company.getName())) {
                    log.debug("[LinkedIn] Skipping mismatch company '{}' for target '{}'", cardCompany, company.getName());
                    continue;
                }

                // Title
                Element titleEl = card.selectFirst("h3.base-search-card__title, h3");
                String title = titleEl != null ? titleEl.text().trim() : "";
                if (!isRelevant(title)) continue;

                // Location
                Element locEl = card.selectFirst(".job-search-card__location, .base-search-card__metadata");
                String location = locEl != null ? locEl.text().trim() : null;

                // Fetch the individual JD for description
                String description = fetchDescription(jobId);

                results.add(new RawJob(
                        company.getName(),
                        jobId,
                        title,
                        description,
                        location,
                        "https://www.linkedin.com/jobs/view/" + jobId
                ));
            }

            log.info("[LinkedIn] {}: {} relevant jobs collected", company.getName(), results.size());
            return results;

        } catch (Exception e) {
            log.warn("[LinkedIn] '{}' failed: {}", company.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Serialized HTTP GET with exponential backoff on HTTP 429.
     */
    private String getWithRetry(String url) throws IOException, InterruptedException {
        int maxRetries = 3;
        long backoff = 2000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            synchronized (LINKEDIN_LOCK) {
                // Minimum spacing between consecutive LinkedIn requests
                Thread.sleep(1200);
                try {
                    return get(url);
                } catch (IOException e) {
                    if (e.getMessage() != null && e.getMessage().contains("HTTP 429")) {
                        if (attempt < maxRetries) {
                            log.warn("[LinkedIn] 429 rate limited, backing off for {} ms (attempt {}/{})", backoff, attempt, maxRetries);
                        } else {
                            throw e;
                        }
                    } else {
                        throw e;
                    }
                }
            }
            Thread.sleep(backoff);
            backoff *= 2;
        }
        throw new IOException("Failed after retries");
    }

    /**
     * Fetches the full job description for a single LinkedIn job posting.
     * Returns an empty string on any error — description is optional.
     */
    private String fetchDescription(String jobId) {
        try {
            String html = getWithRetry(JD_URL + jobId);
            Document doc = Jsoup.parse(html);

            // Primary description block
            Element desc = doc.selectFirst(
                    "div.description__text, " +
                    "div.show-more-less-html__markup, " +
                    "section.description");
            if (desc != null) {
                return desc.text().trim();
            }

            // Fallback: criteria section
            Element criteria = doc.selectFirst("ul.description__job-criteria-list");
            return criteria != null ? criteria.text().trim() : "";

        } catch (Exception e) {
            log.debug("[LinkedIn] JD fetch failed for jobId={}: {}", jobId, e.getMessage());
            return "";
        }
    }

    private boolean matchesTargetCompany(String actualCompany, String targetCompany) {
        if (actualCompany == null || actualCompany.isBlank() || targetCompany == null || targetCompany.isBlank()) {
            return false;
        }
        String actual = actualCompany.trim().toLowerCase();
        String target = targetCompany.trim().toLowerCase();

        // Exact match check
        if (actual.equals(target)) {
            return true;
        }

        // Special aliases for common tech companies
        if (target.equals("meta") && (actual.equals("facebook") || actual.startsWith("meta "))) {
            return true;
        }
        if (target.equals("alphabet") || (target.equals("google") && actual.equals("google"))) {
            return true;
        }

        // Word boundary match (e.g. "Amazon Web Services" matches "Amazon", but "Mitel" does not match "Meta")
        String[] actualWords = actual.split("[\\s,.-]+");
        for (String word : actualWords) {
            if (word.equals(target)) {
                return true;
            }
        }

        return false;
    }
}
