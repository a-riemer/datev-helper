package de.bewidata.datev.acsapi;

/**
 * Thrown when the DATEV ACS API returns an unexpected HTTP status code or
 * an I/O error occurs during a request.
 */
public class DATEVAcsApiException extends Exception {

    private final int    statusCode;
    private final String responseBody;

    public DATEVAcsApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode   = statusCode;
        this.responseBody = responseBody;
    }

    public DATEVAcsApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode   = 0;
        this.responseBody = null;
    }

    /** HTTP status code returned by the API, or 0 for I/O errors. */
    public int getStatusCode() { return statusCode; }

    /** Raw response body, or {@code null} for I/O errors. */
    public String getResponseBody() { return responseBody; }

    /** Returns {@code true} if the server rejected the token (HTTP 401). */
    public boolean isUnauthorized() { return statusCode == 401; }
}
