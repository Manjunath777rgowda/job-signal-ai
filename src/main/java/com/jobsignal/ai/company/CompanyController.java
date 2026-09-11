package com.jobsignal.ai.company;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exposes company metadata for the dashboard.
 * GET /api/v1/companies — all active companies with name, sourceType, careerUrl, and priority tier.
 */
@RestController
@RequestMapping("/api/v1")
public class CompanyController {

    // Priority tiers matching the curated target list
    private static final Set<String> PRIORITY_A = Set.of(
            "Google", "Meta", "Microsoft", "Amazon", "Apple", "NVIDIA",
            "Databricks", "Stripe", "Snowflake", "Uber", "Atlassian", "Rubrik", "LinkedIn"
    );
    private static final Set<String> PRIORITY_B = Set.of(
            "JPMorgan Chase", "Adobe", "Salesforce", "Oracle", "Confluent",
            "MongoDB", "ServiceNow", "Airbnb", "CrowdStrike", "Coinbase"
    );
    // Everything else = C

    private final CompanyRepository companyRepo;

    public CompanyController(CompanyRepository companyRepo) {
        this.companyRepo = companyRepo;
    }

    @GetMapping("/companies")
    public ResponseEntity<List<Map<String, Object>>> list() {
        List<Map<String, Object>> result = companyRepo.findAllByActiveTrue().stream()
                .sorted((a, b) -> {
                    int pa = tier(a.getName()), pb = tier(b.getName());
                    return pa != pb ? Integer.compare(pa, pb) : a.getName().compareToIgnoreCase(b.getName());
                })
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name",       c.getName());
                    m.put("careerUrl",  c.getCareerUrl());
                    m.put("sourceType", c.getSourceType() != null ? c.getSourceType().name() : null);
                    m.put("priority",   tierLabel(c.getName()));
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    private int tier(String name) {
        if (PRIORITY_A.contains(name)) return 1;
        if (PRIORITY_B.contains(name)) return 2;
        return 3;
    }

    private String tierLabel(String name) {
        if (PRIORITY_A.contains(name)) return "A";
        if (PRIORITY_B.contains(name)) return "B";
        return "C";
    }
}
