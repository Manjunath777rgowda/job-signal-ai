package com.jobsignal.ai.ingestion.collector;

import com.jobsignal.ai.company.Company;
import com.jobsignal.ai.company.SourceType;
import com.jobsignal.ai.ingestion.RawJob;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Abstract base for all company-specific job collectors.
 *
 * Each concrete subclass handles one {@link SourceType} and implements
 * {@link #collect(Company)} to return normalised {@link RawJob} objects.
 *
 * Shared utilities (HTTP fetch, HTML strip, relevance filter) live here so
 * subclasses contain only parsing logic.
 */
public abstract class CompanyCollector {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** The source type this collector handles. */
    public abstract SourceType sourceType();

    /**
     * Fetch and return relevant jobs for the given company.
     * Must never throw — return an empty list on failure.
     */
    public abstract List<RawJob> collect(Company company);

    // ── Shared utilities ─────────────────────────────────────────────────────

    /**
     * Returns true if the job title is relevant to the resume profile
     * (backend / platform / ML / DevOps engineering roles).
     */
    protected boolean isRelevant(String title) {
        if (title == null || title.isBlank()) return false;
        String t = title.toLowerCase();
        return t.contains("software engineer")  || t.contains("backend engineer")
            || t.contains("java engineer")      || t.contains("platform engineer")
            || t.contains("staff engineer")     || t.contains("principal engineer")
            || t.contains("senior engineer")    || t.contains("software developer")
            || t.contains("systems engineer")   || t.contains("site reliability")
            || t.contains("devops engineer")    || t.contains("cloud engineer")
            || t.contains("ml engineer")        || t.contains("data engineer")
            || t.contains("api engineer")       || t.contains("full stack engineer")
            || t.contains("full-stack engineer");
    }

    /** Strip HTML tags from a description string. */
    protected String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        return Jsoup.parse(html).text();
    }

    /**
     * Perform a GET request and return the response body as a String.
     *
     * @throws IOException          on HTTP 4xx/5xx or network error
     * @throws InterruptedException if the thread is interrupted
     */
    protected String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                        + "AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/html, */*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .GET()
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }
        return response.body();
    }
}
