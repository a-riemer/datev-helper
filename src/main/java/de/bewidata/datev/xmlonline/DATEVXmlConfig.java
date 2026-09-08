package de.bewidata.datev.xmlonline;

/**
 * Configuration for the DATEV XML interface online (XML-Schnittstelle online).
 *
 * <p>Use the {@link Builder} to create instances:
 * <pre>
 * DATEVXmlConfig config = DATEVXmlConfig.builder()
 *     .consultantNumber(12345)
 *     .clientNumber(1)
 *     .clientName("Musterfirma GmbH")
 *     .build();
 * </pre>
 */
public final class DATEVXmlConfig {

    /** DATEV recommendation: max 100 MB per ZIP file. */
    public static final long DEFAULT_MAX_ZIP_SIZE = 100L * 1024 * 1024;

    /** DATEV limit: max 20 MB per individual document. */
    public static final long DEFAULT_MAX_DOCUMENT_SIZE = 20L * 1024 * 1024;

    /** Beraternummer */
    private final Long consultantNumber;

    /** Mandantennummer */
    private final Long clientNumber;

    /** Mandantenname */
    private final String clientName;

    private final String description;
    private final long maxZipSize;
    private final long maxDocumentSize;

    private DATEVXmlConfig(Builder b) {
        this.consultantNumber = b.consultantNumber;
        this.clientNumber     = b.clientNumber;
        this.clientName       = b.clientName;
        this.description      = b.description;
        this.maxZipSize       = b.maxZipSize;
        this.maxDocumentSize  = b.maxDocumentSize;
    }

    /**
     * DATEV consultant number (Beraternummer).
     * If set and mismatches the import target, DATEV rejects the import.
     */
    public Long getConsultantNumber() { return consultantNumber; }

    /** DATEV client number (Mandantennummer). */
    public Long getClientNumber()     { return clientNumber; }

    /** Client name (Mandantenname), max 36 characters. */
    public String getClientName()     { return clientName; }

    public String getDescription()    { return description; }
    public long getMaxZipSize()       { return maxZipSize; }
    public long getMaxDocumentSize()  { return maxDocumentSize; }

    public static Builder builder()   { return new Builder(); }

    public static final class Builder {
        private Long consultantNumber;
        private Long clientNumber;
        private String clientName;
        private String description;
        private long maxZipSize      = DEFAULT_MAX_ZIP_SIZE;
        private long maxDocumentSize = DEFAULT_MAX_DOCUMENT_SIZE;

        /** Beraternummer */
        public Builder consultantNumber(long number)  { this.consultantNumber = number; return this; }

        /** Mandantennummer */
        public Builder clientNumber(long number)      { this.clientNumber = number; return this; }

        /** Mandantenname (max 36 characters) */
        public Builder clientName(String name)        { this.clientName = name; return this; }

        public Builder description(String text)       { this.description = text; return this; }
        public Builder maxZipSize(long bytes)         { this.maxZipSize = bytes; return this; }
        public Builder maxDocumentSize(long bytes)    { this.maxDocumentSize = bytes; return this; }

        public DATEVXmlConfig build()                 { return new DATEVXmlConfig(this); }
    }
}
