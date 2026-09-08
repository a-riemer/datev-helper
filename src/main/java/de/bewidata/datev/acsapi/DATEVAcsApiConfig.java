package de.bewidata.datev.acsapi;

/**
 * Configuration for the DATEV ACS (accounting) REST APIs.
 *
 * <p>The Bearer token must be obtained externally (e.g. via OAuth 2.0 Authorization
 * Code Flow with PKCE) and passed in here. This class performs no authentication.
 *
 * <p>Usage:
 * <pre>
 * DATEVAcsApiConfig config = DATEVAcsApiConfig.builder()
 *     .token(bearerToken)
 *     .appClientId("your-oauth-client-id")
 *     .datevClientId("455148-1")
 *     .sandbox(true)
 *     .build();
 * </pre>
 */
public final class DATEVAcsApiConfig {

    private final String  token;
    private final String  appClientId;
    private final String  datevClientId;
    private final boolean sandbox;

    private DATEVAcsApiConfig(Builder b) {
        this.token         = b.token;
        this.appClientId   = b.appClientId;
        this.datevClientId = b.datevClientId;
        this.sandbox       = b.sandbox;
    }

    /** OAuth Bearer token (without the "Bearer " prefix). */
    public String getToken() { return token; }

    /** OAuth App Client ID — sent as the {@code X-DATEV-Client-Id} request header. */
    public String getAppClientId() { return appClientId; }

    /** DATEV Mandant-/Client-ID used as URL path parameter (e.g. {@code "455148-1"}). */
    public String getDatevClientId() { return datevClientId; }

    public boolean isSandbox() { return sandbox; }

    /**
     * Returns a copy of this config with the given DATEV client ID, all other settings unchanged.
     * Used by {@link DATEVApiProcessor} to inject a client ID derived from the EXTF CSV header.
     */
    public DATEVAcsApiConfig withDatevClientId(String id) {
        return new Builder()
                .token(this.token)
                .appClientId(this.appClientId)
                .datevClientId(id)
                .sandbox(this.sandbox)
                .build();
    }

    private String basePath() {
        return sandbox ? "platform-sandbox" : "platform";
    }

    /** Base URL for the accounting:documents API (Buchungsdatenservice document archive). */
    public String documentsBaseUrl() {
        return "https://accounting-documents.api.datev.de/" + basePath() + "/v2";
    }

    /** Base URL for the accounting:extf-files API (Buchungsdatenservice EXTF import). */
    public String extfFilesBaseUrl() {
        return "https://accounting-extf-files.api.datev.de/" + basePath() + "/v3";
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String  token;
        private String  appClientId;
        private String  datevClientId;
        private boolean sandbox = false;

        public Builder token(String token)               { this.token         = token; return this; }
        public Builder appClientId(String id)            { this.appClientId   = id;    return this; }
        public Builder datevClientId(String id)          { this.datevClientId = id;    return this; }
        public Builder sandbox(boolean sandbox)          { this.sandbox       = sandbox; return this; }

        public DATEVAcsApiConfig build() {
            if (token == null || token.isBlank())
                throw new IllegalStateException("token is required");
            if (appClientId == null || appClientId.isBlank())
                throw new IllegalStateException("appClientId is required");
            return new DATEVAcsApiConfig(this);
        }
    }
}
