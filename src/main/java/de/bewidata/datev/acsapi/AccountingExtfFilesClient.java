package de.bewidata.datev.acsapi;

import de.bewidata.datev.acsapi.internal.SimpleJson;
import de.bewidata.datev.acsapi.model.ExtfJobStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Client for the DATEV {@code accounting:extf-files} API — EXTF CSV import of the
 * Buchungsdatenservice.
 *
 * <p>Base URL:
 * <ul>
 *   <li>Live:    {@code https://accounting-extf-files.api.datev.de/platform/v3}</li>
 *   <li>Sandbox: {@code https://accounting-extf-files.api.datev.de/platform-sandbox/v3}</li>
 * </ul>
 *
 * <p>Required OAuth scopes: {@code datev:accounting:extf-files-import}
 *
 * <p>Typical workflow:
 * <pre>
 * AccountingExtfFilesClient client = new AccountingExtfFilesClient(config);
 * String jobId = client.importExtf(Path.of("buchungsstapel.csv"));
 * ExtfJobStatus status = client.awaitCompletion(jobId, Duration.ofMinutes(2));
 * if (status.isError()) System.err.println("Import errors: " + status);
 * </pre>
 */
public class AccountingExtfFilesClient {

    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(5);

    private final DATEVAcsApiConfig config;
    private final HttpClient        http;
    private final Duration          pollInterval;

    public AccountingExtfFilesClient(DATEVAcsApiConfig config) {
        this(config, DEFAULT_POLL_INTERVAL);
    }

    public AccountingExtfFilesClient(DATEVAcsApiConfig config, Duration pollInterval) {
        this.config       = config;
        this.http         = HttpClient.newHttpClient();
        this.pollInterval = pollInterval;
    }

    /**
     * Uploads an EXTF CSV file and returns the job ID for status polling.
     *
     * <p>The file is sent as raw binary ({@code application/octet-stream}).
     * The job ID is extracted from the {@code Location} response header.
     *
     * @param csvFile path to the EXTF CSV file
     * @return job ID assigned by DATEV
     * @throws DATEVAcsApiException if the API returns a non-2xx status or the Location header
     *                              cannot be parsed
     */
    public String importExtf(Path csvFile) throws DATEVAcsApiException, IOException, InterruptedException {
        String url = config.extfFilesBaseUrl()
                + "/clients/" + config.getDatevClientId()
                + "/extf-files/import";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization",    "Bearer " + config.getToken())
                .header("X-DATEV-Client-Id", config.getAppClientId())
                .header("Content-Type",      "application/octet-stream")
                .header("Filename",          csvFile.getFileName().toString())
                .POST(HttpRequest.BodyPublishers.ofFile(csvFile))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new DATEVAcsApiException(
                    "EXTF import failed: HTTP " + response.statusCode() + " — " + response.body(),
                    response.statusCode(), response.body());
        }

        return extractJobId(response);
    }

    /**
     * Retrieves the current status of an EXTF import job.
     *
     * @param jobId the job ID returned by {@link #importExtf}
     * @throws DATEVAcsApiException if the API returns a non-2xx status
     */
    public ExtfJobStatus getJobStatus(String jobId)
            throws DATEVAcsApiException, IOException, InterruptedException {

        String url = config.extfFilesBaseUrl()
                + "/clients/" + config.getDatevClientId()
                + "/extf-files/jobs/" + jobId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization",    "Bearer " + config.getToken())
                .header("X-DATEV-Client-Id", config.getAppClientId())
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new DATEVAcsApiException(
                    "Job status request failed: HTTP " + response.statusCode() + " — " + response.body(),
                    response.statusCode(), response.body());
        }

        int statusCode = SimpleJson.getInt(response.body(), "status");
        return new ExtfJobStatus(jobId, statusCode, response.body());
    }

    /**
     * Polls {@link #getJobStatus} every 5 seconds until the job reaches a terminal state
     * or {@code timeout} elapses.
     *
     * @param jobId   job ID to poll
     * @param timeout maximum wait time
     * @return the final {@link ExtfJobStatus}
     * @throws DATEVAcsApiException if a status request fails or the timeout is exceeded
     */
    public ExtfJobStatus awaitCompletion(String jobId, Duration timeout)
            throws DATEVAcsApiException, IOException, InterruptedException {

        Instant deadline = Instant.now().plus(timeout);
        while (true) {
            ExtfJobStatus status = getJobStatus(jobId);
            if (status.isTerminal()) return status;
            if (Instant.now().isAfter(deadline)) {
                throw new DATEVAcsApiException(
                        "Timeout waiting for job " + jobId + " after " + timeout.toSeconds() + "s"
                        + " (last status=" + status.statusCode() + ")",
                        0, null);
            }
            Thread.sleep(pollInterval.toMillis());
        }
    }

    // ---- helpers ----

    private static String extractJobId(HttpResponse<String> response) throws DATEVAcsApiException {
        String location = response.headers().firstValue("Location").orElse(null);
        if (location == null || location.isBlank()) {
            throw new DATEVAcsApiException(
                    "EXTF import response has no Location header (HTTP " + response.statusCode() + ")",
                    response.statusCode(), response.body());
        }
        // Job ID is the last path segment of the Location URL.
        String path = location;
        try {
            path = URI.create(location).getPath();
        } catch (IllegalArgumentException ignored) {
            // Use location string as-is if it is not a valid URI.
        }
        if (path == null || path.isEmpty()) path = location;
        int slash = path.lastIndexOf('/');
        String jobId = slash >= 0 ? path.substring(slash + 1) : path;
        if (jobId.isEmpty()) {
            throw new DATEVAcsApiException(
                    "Cannot extract job ID from Location header: " + location,
                    response.statusCode(), response.body());
        }
        return jobId;
    }
}
