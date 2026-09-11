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
 * Fetches jobs from Oracle Careers public search endpoint.
 * Endpoint: https://careers.oracle.com/jobs/search
 *
 * No board_slug required — the endpoint is fixed.
 */
@Component
public class OracleCollector extends CompanyCollector {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SourceType sourceType() {
        return SourceType.ORACLE;
    }

    @Override
    public List<RawJob> collect(Company company) {
        String url = "https://careers.oracle.com/jobs/search"
                + "?term=software+engineer&rows=20&start=0&category=information-technology";
        try {
            JsonNode root = mapper.readTree(get(url));

            // Oracle may return jobs under different keys depending on API version
            JsonNode jobs = root.path("requisitionList");
            if (!jobs.isArray() || jobs.isEmpty()) {
                jobs = root.path("jobs");
            }

            List<RawJob> results = new ArrayList<>();
            for (JsonNode job : jobs) {
                String title = job.path("title").asText(job.path("Title").asText(""));
                if (!isRelevant(title)) continue;

                String id   = job.path("id").asText(job.path("Id").asText(""));
                String loc  = job.path("primaryLocation").asText(job.path("PrimaryLocation").asText(null));
                String desc = job.path("description").asText(job.path("ShortDescription").asText(""));
                String link = "https://careers.oracle.com/jobs/#en/sites/jobsearch/job/" + id;

                results.add(new RawJob(company.getName(), id, title, desc, loc, link));
            }
            log.info("[Oracle] {}: {} relevant jobs collected", company.getName(), results.size());
            return results;

        } catch (Exception e) {
            log.warn("[Oracle] failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
