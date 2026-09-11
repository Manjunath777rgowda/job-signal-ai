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
 * Fetches jobs from the Lever public postings API.
 * URL pattern: https://api.lever.co/v0/postings/{board_slug}?mode=json
 *
 * The board_slug is stored in companies.board_slug.
 * Companies using this source: Apple, LinkedIn.
 */
@Component
public class LeverCollector extends CompanyCollector {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SourceType sourceType() {
        return SourceType.LEVER;
    }

    @Override
    public List<RawJob> collect(Company company) {
        String slug = company.getBoardSlug();
        if (slug == null || slug.isBlank()) {
            log.warn("[Lever] '{}': board_slug not configured — skipping", company.getName());
            return Collections.emptyList();
        }

        String url = "https://api.lever.co/v0/postings/" + slug + "?mode=json&limit=50";
        try {
            JsonNode jobs = mapper.readTree(get(url));

            List<RawJob> results = new ArrayList<>();
            for (JsonNode job : jobs) {
                String title = job.path("text").asText("");
                if (!isRelevant(title)) continue;

                String desc = job.path("descriptionPlain").asText(null);
                if (desc == null || desc.isBlank()) {
                    desc = stripHtml(job.path("description").asText(""));
                }

                results.add(new RawJob(
                        company.getName(),
                        job.path("id").asText(""),
                        title,
                        desc,
                        job.path("categories").path("location").asText(null),
                        job.path("hostedUrl").asText(null)
                ));
            }
            log.info("[Lever] {}: {} relevant jobs collected", company.getName(), results.size());
            return results;

        } catch (Exception e) {
            log.warn("[Lever] '{}' failed: {}", company.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }
}
