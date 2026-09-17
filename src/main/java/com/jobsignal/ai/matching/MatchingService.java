package com.jobsignal.ai.matching;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic matching engine.
 *
 * Weights:
 *   Skills           30%
 *   Experience       20%
 *   Responsibilities 20%
 *   Domain           20%  (was 10%; absorbed the removed Seniority 10%)
 *   Location          5%
 *   Other             5%
 *
 * The profile data is derived directly from the uploaded resume (Manjunath R).
 */
@Service
public class MatchingService {

    // ── Primary skills with evidence from resume ────────────────────────────
    private static final Set<String> PRIMARY_SKILLS = Set.of(
            "java", "spring boot", "spring", "microservices", "rest api", "rest apis",
            "postgresql", "postgres", "docker", "kubernetes", "jenkins",
            "keycloak", "alembic", "python", "mysql", "mongodb", "redis",
            "aws", "azure", "gcp", "cloud", "bigquery", "big query"
    );

    // ── Secondary / ML skills ────────────────────────────────────────────────
    private static final Set<String> SECONDARY_SKILLS = Set.of(
            "machine learning", "nlp", "natural language processing",
            "computer vision", "svm", "random forest", "regression",
            "pytorch", "tensorflow", "scikit", "pandas", "numpy"
    );

    // ── Tools & other from resume ────────────────────────────────────────────
    private static final Set<String> OTHER_SKILLS = Set.of(
            "maven", "git", "linux", "prometheus", "grafana", "graylog",
            "dsa", "data structures", "oops", "object oriented",
            "distributed systems", "system design", "architecture", "api"
    );

    // ── Backend / Platform domain keywords ─────────────────────────────────
    private static final Set<String> MATCHING_DOMAINS = Set.of(
            "backend", "back-end", "back end", "platform", "infrastructure",
            "distributed", "microservice", "api", "cloud", "devops",
            "engineering", "system"
    );

    // ── Responsibility keywords from resume ─────────────────────────────────
    private static final Set<String> RESPONSIBILITY_KEYWORDS = Set.of(
            "design", "develop", "architect", "lead", "mentor", "scale",
            "deploy", "optimize", "integrate", "migrate", "build", "implement",
            "collaborate", "review", "ownership"
    );

    // ── Preferred locations ─────────────────────────────────────────────────
    private static final Set<String> PREFERRED_LOCATIONS = Set.of(
            "bengaluru", "bangalore", "india", "remote", "hybrid"
    );

    // ── Skills NOT on the resume that should be flagged as gaps ─────────────
    private static final Set<String> KNOWN_GAP_SKILLS = Set.of(
            "aws lambda", "react", "typescript", "javascript", "angular", "vue",
            "golang", "go lang", "rust", "scala", "kafka", "spark",
            "hadoop", "flink", "c++", "swift", "objective-c", "ruby on rails",
            "pytorch", ".net", "asp.net"
    );

    public MatchAnalysisResponse analyze(MatchAnalysisRequest request) {
        String text    = normalize(request.getJobDescription() + " " + request.getJobTitle());

        List<String> strengths = new ArrayList<>();
        List<String> gaps      = new ArrayList<>();

        // ── 1. Skill Score (30%) ─────────────────────────────────────────────
        int primaryHits   = countHits(text, PRIMARY_SKILLS,   strengths);
        int secondaryHits = countHits(text, SECONDARY_SKILLS, strengths);
        int otherHits     = countHits(text, OTHER_SKILLS,     strengths);

        int skillScore = Math.min(100,
                (primaryHits * 12) + (secondaryHits * 5) + (otherHits * 3));

        // ── 2. Experience Score (20%) ─────────────────────────────────────────
        int experienceScore = 80; // solid baseline for 8-year engineer
        if (text.contains("10 years") || text.contains("10+ years") || text.contains("12 years")) {
            experienceScore = 60;
            gaps.add("10+ years experience required");
        } else if (text.contains("5 years") || text.contains("5+ years") ||
                   text.contains("7 years") || text.contains("7+ years")) {
            experienceScore = 90;
        } else if (text.contains("3 years") || text.contains("3+ years")) {
            experienceScore = 100;
        }

        // ── 3. Responsibility Score (20%) ─────────────────────────────────────
        long respHits = RESPONSIBILITY_KEYWORDS.stream()
                .filter(text::contains)
                .count();
        int responsibilityScore = (int) Math.min(100, respHits * 15);

        // ── 4. Domain Score (20%) ─────────────────────────────────────────────
        // Weight doubled from 10% → 20% after removing the seniority dimension.
        long domainHits = MATCHING_DOMAINS.stream().filter(text::contains).count();
        int domainScore = (int) Math.min(100, domainHits * 15);

        // ── 5. Location Score (5%) ────────────────────────────────────────────
        int locationScore;
        boolean preferredLocation = PREFERRED_LOCATIONS.stream().anyMatch(text::contains);
        boolean remoteOk = text.contains("remote") || text.contains("hybrid") || text.contains("wfh");
        if (preferredLocation || remoteOk) {
            locationScore = 100;
        } else if (text.contains("onsite") || text.contains("on-site")) {
            locationScore = 40;
        } else {
            locationScore = 70;
        }

        // ── 6. Other Score (5%) ───────────────────────────────────────────────
        int otherScore = Math.min(100, otherHits * 10 + 40);

        // ── Weighted overall score ────────────────────────────────────────────
        double overallDouble =
                skillScore          * 0.30 +
                experienceScore     * 0.20 +
                responsibilityScore * 0.20 +
                domainScore         * 0.20 +
                locationScore       * 0.05 +
                otherScore          * 0.05;

        int overallScore = (int) Math.round(Math.min(100, Math.max(0, overallDouble)));

        // ── Gap detection ─────────────────────────────────────────────────────
        for (String gap : KNOWN_GAP_SKILLS) {
            if (text.contains(gap)) {
                gaps.add(toDisplayName(gap));
            }
        }

        if (primaryHits == 0 && secondaryHits == 0) {
            gaps.add("No matching core skills detected");
        }

        // ── Recommendation ────────────────────────────────────────────────────
        String recommendation;
        if (overallScore >= 90) {
            recommendation = "APPLY_NOW";
        } else if (overallScore >= 80) {
            recommendation = "APPLY";
        } else if (overallScore >= 70) {
            recommendation = "CONSIDER";
        } else {
            recommendation = "SKIP";
        }

        // ── Interview Fit ─────────────────────────────────────────────────────
        String interviewFit;
        if (overallScore >= 85) {
            interviewFit = "HIGH";
        } else if (overallScore >= 70) {
            interviewFit = "MEDIUM";
        } else {
            interviewFit = "LOW";
        }

        return new MatchAnalysisResponse(
                overallScore, interviewFit, recommendation,
                skillScore, experienceScore, responsibilityScore,
                domainScore, locationScore, otherScore,
                strengths, gaps
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int countHits(String text, Set<String> skills, List<String> strengths) {
        int count = 0;
        for (String skill : skills) {
            if (text.contains(skill)) {
                strengths.add(toDisplayName(skill));
                count++;
            }
        }
        return count;
    }

    private String toDisplayName(String key) {
        String[] words = key.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }

    String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+\\s]", " ").replaceAll("\\s+", " ").trim();
    }
}
