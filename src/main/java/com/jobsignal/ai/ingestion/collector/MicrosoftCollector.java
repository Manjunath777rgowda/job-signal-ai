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
 * Fetches jobs from the Microsoft Careers public search API.
 * Endpoint: https://jobs.careers.microsoft.com/global/en/search
 *
 * No board_slug required — the endpoint is fixed.
 */
@Component
public class MicrosoftCollector extends CompanyCollector {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SourceType sourceType() {
        return SourceType.MICROSOFT;
    }

    @Override
    public List<RawJob> collect(Company company) {
        String url = "https://jobs.careers.microsoft.com/global/en/search"
                + "?q=software%20engineer&lc=India&l=en_us&pg=1&pgSz=20&o=Relevance&flt=true";
        try {
            JsonNode root = mapper.readTree(get(url));
            JsonNode jobs = root.path("operationResult").path("result").path("jobs");

            if (!jobs.isArray()) {
                log.warn("[Microsoft] Unexpected response shape — no jobs array found");
                return Collections.emptyList();
            }

            List<RawJob> results = new ArrayList<>();
            for (JsonNode job : jobs) {
                String title = job.path("title").asText("");
                if (!isRelevant(title)) continue;

                String id = job.path("jobId").asText("");
                results.add(new RawJob(
                        company.getName(),
                        id,
                        title,
                        job.path("description").asText(""),
                        job.path("primaryLocation").asText(null),
                        "https://jobs.careers.microsoft.com/global/en/job/" + id
                ));
            }
            log.info("[Microsoft] {}: {} relevant jobs collected", company.getName(), results.size());
            return results;

        } catch (Exception e) {
            log.warn("[Microsoft] failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
