package de.bewidata.datev.acsapi.model;

/**
 * Metadata sent alongside a document uploaded to the DATEV Buchungsdatenservice archive.
 *
 * @param category application or source system name (e.g. {@code "MÖBELPILOT"})
 * @param folder   target folder in the DATEV document archive (e.g. {@code "Belege"})
 * @param register accounting period in {@code YYYY-MM} format (e.g. {@code "2025-01"})
 */
public record DocumentUploadMetadata(String category, String folder, String register) {

    /** Serialises this record to a JSON object string suitable for the API {@code metadata} field. */
    public String toJson() {
        return "{\"category\":" + jsonString(category)
             + ",\"folder\":"   + jsonString(folder)
             + ",\"register\":" + jsonString(register) + "}";
    }

    private static String jsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
