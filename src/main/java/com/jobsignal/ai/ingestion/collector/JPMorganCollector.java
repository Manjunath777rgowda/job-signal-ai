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
 * Fetches jobs from JPMorgan Chase via their Oracle Cloud HCM public search API.
 *
 * Correct endpoint:
 *   https://jpmc.fa.oraclecloud.com/hcmRestApi/resources/latest/recruitingCEJobRequisitions
 *   ?onlyData=true&expand=requisitionList&finder=findReqs;siteNumber=CX&limit=20&offset=0
 *
 * The response structure is:
 *   items[0].requisitionList[].{ Id, Title, ShortDescriptionStr, PrimaryLocation, PostedDate }
 */
@Component
public class JPMorganCollector extends CompanyCollector {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SourceType sourceType() {
        return SourceType.JPMORGAN;
    }

    @Override
    public List<RawJob> collect(Company company) {
        // The finder param must use semicolons inside the value (not commas for this endpoint)
        String url = "https://jpmc.fa.oraclecloud.com/hcmRestApi/resources/latest/recruitingCEJobRequisitions"
                + "?onlyData=true"
                + "&expand=requisitionList"
                + "&finder=findReqs%3BsiteNumber%3DCX"   // findReqs;siteNumber=CX
                + "&limit=25&offset=0";
        try {
            JsonNode root  = mapper.readTree(get(url));
            JsonNode items = root.path("items");

            List<RawJob> results = new ArrayList<>();
            for (JsonNode item : items) {
                for (JsonNode req : item.path("requisitionList")) {
                    String title = req.path("Title").asText("").trim();
                    if (title.isBlank() || !isRelevant(title)) continue;

                    String id   = req.path("Id").asText("").trim();
                    if (id.isBlank()) continue;

                    // ShortDescriptionStr is the plain-text synopsis; fall back to criteria strings
                    String desc = req.path("ShortDescriptionStr").asText(
                                  req.path("ExternalResponsibilitiesStr").asText(""));

                    String loc  = req.path("PrimaryLocation").asText(null);
                    String link = "https://jpmc.fa.oraclecloud.com/hcmUI/CandidateExperience"
                                + "/en/sites/CX/requisitions/preview/" + id;

                    results.add(new RawJob(company.getName(), id, title, desc, loc, link));
                }
            }
            log.info("[JPMorgan] {}: {} relevant jobs collected", company.getName(), results.size());
            return results;

        } catch (Exception e) {
            log.error("[JPMorgan] failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
