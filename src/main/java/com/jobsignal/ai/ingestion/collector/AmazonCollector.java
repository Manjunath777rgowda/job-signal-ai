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
 * Fetches jobs from the Amazon Jobs public search API.
 * Endpoint: https://www.amazon.jobs/en/search.json
 *
 * No board_slug required — the endpoint is fixed.
 */
@Component
public class AmazonCollector extends CompanyCollector {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SourceType sourceType() {
        return SourceType.AMAZON;
    }

    @Override
    public List<RawJob> collect(Company company) {
        String url = "https://www.amazon.jobs/en/search.json"
                + "?base_query=software+engineer+java"
                + "&category%5B%5D=software-development"
                + "&result_limit=20&offset=0";
        try {
            JsonNode root = mapper.readTree(get(url));
            JsonNode jobs = root.path("jobs");

            List<RawJob> results = new ArrayList<>();
            for (JsonNode job : jobs) {
                String title = job.path("title").asText("");
                if (!isRelevant(title)) continue;

                String id      = job.path("id").asText(job.path("job_id").asText(""));
                String jobPath = job.path("job_path").asText("");
                String jobUrl  = jobPath.isBlank() ? null : "https://www.amazon.jobs" + jobPath;

                results.add(new RawJob(
                        company.getName(),
                        id,
                        title,
                        stripHtml(job.path("description").asText("")),
                        job.path("location").asText(null),
                        jobUrl
                ));
            }
            log.info("[Amazon] {}: {} relevant jobs collected", company.getName(), results.size());
            return results;

        } catch (Exception e) {
            log.error("[Amazon] failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
