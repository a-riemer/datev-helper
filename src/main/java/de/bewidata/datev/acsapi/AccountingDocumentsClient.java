package de.bewidata.datev.acsapi;

import de.bewidata.datev.acsapi.internal.MultipartBody;
import de.bewidata.datev.acsapi.internal.SimpleJson;
import de.bewidata.datev.acsapi.model.DocumentUploadMetadata;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Client for the DATEV {@code accounting:documents} API — document archive of the
 * Buchungsdatenservice.
 *
 * <p>Base URL:
 * <ul>
 *   <li>Live:    {@code https://accounting-documents.api.datev.de/platform/v2}</li>
 *   <li>Sandbox: {@code https://accounting-documents.api.datev.de/platform-sandbox/v2}</li>
 * </ul>
 *
 * <p>Required OAuth scope: {@code accounting:documents}
 *
 * <p>Example:
 * <pre>
 * AccountingDocumentsClient client = new AccountingDocumentsClient(config);
 * DocumentUploadMetadata meta = new DocumentUploadMetadata("MyApp", "Belege", "2025-01");
 * client.uploadWithGuid(guid, Path.of("invoice.pdf"), meta);
 * </pre>
 */
public class AccountingDocumentsClient {

    private final DATEVAcsApiConfig config;
    private final HttpClient        http;

    public AccountingDocumentsClient(DATEVAcsApiConfig config) {
        this.config = config;
        this.http   = HttpClient.newHttpClient();
    }

    /**
     * Uploads a document with a caller-supplied GUID ({@code PUT /documents/{guid}}).
     *
     * <p>Preferred approach when the GUID is already embedded in the EXTF CSV:
     * documents and the CSV file can be prepared in parallel without waiting for responses.
     *
     * @param guid GUID to assign (UUID string, will be used verbatim as the path parameter)
     * @param file local file to upload (typically a PDF)
     * @param meta archive metadata (category, folder, accounting period)
     * @throws DATEVAcsApiException if the API returns a non-2xx status
     */
    public void uploadWithGuid(String guid, Path file, DocumentUploadMetadata meta)
            throws DATEVAcsApiException, IOException, InterruptedException {

        String boundary = newBoundary();
        byte[] body = buildMultipart(boundary, file, meta);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(documentsUrl() + "/" + guid))
                .header("Authorization",    "Bearer " + config.getToken())
                .header("X-DATEV-Client-Id", config.getAppClientId())
                .header("Content-Type",      "multipart/form-data; boundary=" + boundary)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response, "PUT /documents/" + guid);
    }

    /**
     * Uploads a document and lets DATEV assign the GUID ({@code POST /documents}).
     *
     * @param file local file to upload
     * @param meta archive metadata
     * @return GUID assigned by DATEV
     * @throws DATEVAcsApiException if the API returns a non-2xx status or the response
     *                              contains no {@code guid} field
     */
    public String upload(Path file, DocumentUploadMetadata meta)
            throws DATEVAcsApiException, IOException, InterruptedException {

        String boundary = newBoundary();
        byte[] body = buildMultipart(boundary, file, meta);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(documentsUrl()))
                .header("Authorization",    "Bearer " + config.getToken())
                .header("X-DATEV-Client-Id", config.getAppClientId())
                .header("Content-Type",      "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response, "POST /documents");

        String guid = SimpleJson.getString(response.body(), "guid");
        if (guid == null || guid.isBlank()) {
            throw new DATEVAcsApiException(
                    "Document upload succeeded but response contains no 'guid' field: " + response.body(),
                    response.statusCode(), response.body());
        }
        return guid;
    }

    // ---- helpers ----

    private String documentsUrl() {
        return config.documentsBaseUrl() + "/clients/" + config.getDatevClientId() + "/documents";
    }

    private static byte[] buildMultipart(String boundary, Path file, DocumentUploadMetadata meta)
            throws IOException {
        return new MultipartBody(boundary)
                .addText("metadata", meta.toJson(), "application/json")
                .addFile("file", file.getFileName().toString(), file)
                .finish();
    }

    private static String newBoundary() {
        return "----DATEVBoundary" + UUID.randomUUID().toString().replace("-", "");
    }

    private static void checkStatus(HttpResponse<String> response, String operation)
            throws DATEVAcsApiException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new DATEVAcsApiException(
                    operation + " failed: HTTP " + response.statusCode() + " — " + response.body(),
                    response.statusCode(), response.body());
        }
    }
}
