package com.jobsignal.ai.ingestion.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsignal.ai.company.Company;
import com.jobsignal.ai.company.SourceType;
import com.jobsignal.ai.ingestion.RawJob;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fetches jobs from the Greenhouse public job board API.
 * URL pattern: https://boards-api.greenhouse.io/v1/boards/{board_slug}/jobs?content=true
 *
 * The board_slug is stored in companies.board_slug.
 * Companies using this source: Meta, Atlassian, Adobe, Uber, Salesforce,
 *   Databricks, Stripe, Snowflake, Rubrik, Confluent, MongoDB, ServiceNow,
 *   Airbnb, CrowdStrike, Coinbase, Cisco, Intuit, PayPal, Visa, Mastercard.
 */
@Component
public class GreenhouseCollector extends CompanyCollector {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SourceType sourceType() {
        return SourceType.GREENHOUSE;
    }

    @Override
    public List<RawJob> collect(Company company) {
        String slug = company.getBoardSlug();
        if (slug == null || slug.isBlank()) {
            log.warn("[Greenhouse] '{}': board_slug not configured — skipping", company.getName());
            return Collections.emptyList();
        }

        String url = "https://boards-api.greenhouse.io/v1/boards/" + slug + "/jobs?content=true";
        try {
            JsonNode root = mapper.readTree(get(url));
            JsonNode jobs = root.path("jobs");

            List<RawJob> results = new ArrayList<>();
            for (JsonNode job : jobs) {
                String title = job.path("title").asText("");
                if (!isRelevant(title)) continue;

                results.add(new RawJob(
                        company.getName(),
                        String.valueOf(job.path("id").asLong()),
                        title,
                        stripHtml(job.path("content").asText("")),
                        job.path("location").path("name").asText(null),
                        job.path("absolute_url").asText(null)
                ));
            }
            log.info("[Greenhouse] {}: {} relevant jobs collected", company.getName(), results.size());
            return results;

        } catch (Exception e) {
            log.warn("[Greenhouse] '{}' failed: {}", company.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }
}
