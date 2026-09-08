package de.bewidata.datev.acsapi.model;

/**
 * Processing status of an EXTF import job (accounting:extf-files API).
 *
 * <p>DATEV status codes (as observed; verify against current DATEV documentation):
 * <ul>
 *   <li>1–3 — received / in progress</li>
 *   <li>4   — completed successfully (sandbox always returns 4)</li>
 *   <li>5+  — completed with validation errors</li>
 * </ul>
 *
 * <p>The {@code responseBody} contains the raw JSON response from the DATEV API.
 * For error statuses it typically includes validation details that can be displayed to the user.
 * May be {@code null} if the response body was empty or unavailable.
 */
public record ExtfJobStatus(String jobId, int statusCode, String responseBody) {

    /** Returns {@code true} when the job has reached a terminal state (no further polling needed). */
    public boolean isTerminal() { return statusCode >= 4; }

    /** Returns {@code true} when the job completed without errors. */
    public boolean isSuccess()  { return statusCode == 4; }

    /** Returns {@code true} when the job completed with validation errors. */
    public boolean isError()    { return statusCode > 4; }

    @Override
    public String toString() {
        String label = switch (statusCode) {
            case 1, 2, 3 -> "processing";
            case 4       -> "completed successfully";
            default      -> "completed with errors";
        };
        return "Job " + jobId + ": " + label + " (status=" + statusCode + ")";
    }
}
