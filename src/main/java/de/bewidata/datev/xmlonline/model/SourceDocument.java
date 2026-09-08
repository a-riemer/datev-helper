package de.bewidata.datev.xmlonline.model;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A physical document (PDF or other file) to be attached to a booking line.
 * Provided by the {@link de.bewidata.datev.xmlonline.plugin.SourceDocumentProvider} plugin.
 *
 * <p>German accounting term: <em>Buchungsbeleg</em>
 */
public final class SourceDocument {

    /** Belegkreis Rechnungseingang (DATEV: type=1) */
    public static final byte DOCUMENT_TYPE_INCOMING = 1;

    /** Belegkreis Rechnungsausgang (DATEV: type=2) */
    public static final byte DOCUMENT_TYPE_OUTGOING = 2;

    /** Buchungsrelevant – document is placed in the inbox (Posteingang) */
    public static final byte PROCESS_ID_BOOKING = 1;

    /** Archivierungsrelevant – document is filed directly in a folder */
    public static final byte PROCESS_ID_ARCHIVE = 2;

    private final Path localPath;
    private final String fileName;
    private final byte documentType;
    private final byte processId;
    private final String description;
    private final String keywords;

    private SourceDocument(Builder b) {
        this.localPath   = b.localPath;
        this.fileName    = b.fileName != null ? b.fileName : b.localPath.getFileName().toString();
        this.documentType = b.documentType;
        this.processId   = b.processId;
        this.description = b.description;
        this.keywords    = b.keywords;
    }

    public Path getLocalPath()    { return localPath; }

    /** File name as it will appear inside the ZIP archive. */
    public String getFileName()   { return fileName; }

    /** 1 = incoming invoice (Rechnungseingang), 2 = outgoing invoice (Rechnungsausgang) */
    public byte getDocumentType()  { return documentType; }

    /** 1 = booking-relevant (Posteingang), 2 = archive-relevant (Ordner) */
    public byte getProcessId()    { return processId; }

    public String getDescription() { return description; }
    public String getKeywords()    { return keywords; }

    /** Returns the file size in bytes, or 0 if the file cannot be read. */
    public long fileSize() {
        try {
            return Files.size(localPath);
        } catch (java.io.IOException e) {
            return 0L;
        }
    }

    /** Factory for an incoming invoice document (Rechnungseingang). */
    public static Builder incoming(Path path) {
        return new Builder(path, DOCUMENT_TYPE_INCOMING);
    }

    /** Factory for an outgoing invoice document (Rechnungsausgang). */
    public static Builder outgoing(Path path) {
        return new Builder(path, DOCUMENT_TYPE_OUTGOING);
    }

    public static final class Builder {
        private final Path localPath;
        private final byte documentType;
        private String fileName;
        private byte processId = PROCESS_ID_ARCHIVE;
        private String description;
        private String keywords;

        private Builder(Path localPath, byte documentType) {
            this.localPath   = localPath;
            this.documentType = documentType;
        }

        /** Overrides the file name used inside the ZIP (default: file name of localPath). */
        public Builder fileName(String fileName)     { this.fileName = fileName; return this; }

        public Builder processId(byte processId)     { this.processId = processId; return this; }

        /** Marks the document as booking-relevant (placed in Posteingang). */
        public Builder bookingRelevant()             { this.processId = PROCESS_ID_BOOKING; return this; }

        /** Short description, max 40 characters (truncated automatically). */
        public Builder description(String text)      { this.description = text; return this; }

        public Builder keywords(String keywords)     { this.keywords = keywords; return this; }

        public SourceDocument build() {
            if (localPath == null) throw new IllegalStateException("localPath is required");
            return new SourceDocument(this);
        }
    }
}
