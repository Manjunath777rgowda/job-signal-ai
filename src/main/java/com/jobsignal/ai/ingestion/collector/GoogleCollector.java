package com.jobsignal.ai.ingestion.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsignal.ai.company.Company;
import com.jobsignal.ai.company.SourceType;
import com.jobsignal.ai.ingestion.RawJob;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fetches jobs from the Google Careers public search API.
 * Endpoint: https://careers.google.com/api/v3/search/
 *
 * No board_slug required — the endpoint is fixed.
 */
@Component
public class GoogleCollector extends CompanyCollector {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SourceType sourceType() {
        return SourceType.GOOGLE;
    }

    @Override
    public List<RawJob> collect(Company company) {
        // Google Careers public search API — no trailing slash on search endpoint
        String query = URLEncoder.encode("software engineer backend", StandardCharsets.UTF_8);
        String url   = "https://careers.google.com/api/v3/search?query=" + query
                     + "&page_size=20&page=1";
        try {
            JsonNode root = mapper.readTree(get(url));
            JsonNode jobs = root.path("jobs");

            List<RawJob> results = new ArrayList<>();
            for (JsonNode job : jobs) {
                String title = job.path("title").asText("");
                if (!isRelevant(title)) continue;

                String id       = job.path("job_id").asText("");
                String location = job.path("locations").isArray() && job.path("locations").size() > 0
                        ? job.path("locations").get(0).asText(null) : null;

                results.add(new RawJob(
                        company.getName(),
                        id,
                        title,
                        stripHtml(job.path("description").asText("")),
                        location,
                        "https://careers.google.com/jobs/results/" + id
                ));
            }
            log.info("[Google] {}: {} relevant jobs collected", company.getName(), results.size());
            return results;

        } catch (Exception e) {
            log.warn("[Google] failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
