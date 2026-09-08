package de.bewidata.datev.xmlonline.plugin;

import de.bewidata.datev.xmlonline.model.SourceDocument;

import java.util.List;
import java.util.Optional;

/**
 * Result returned by a {@link SourceDocumentProvider}.
 *
 * <p>Contains the resolved source documents for a booking line, an optional
 * externally provided GUID, the find status, and an optional human-readable message.
 *
 * <p>When a GUID is present it is used as the {@code guid} attribute on the
 * {@code <document>} element in {@code document.xml} and as the {@code BEDI+}
 * prefix value in the CSV {@code Beleglink} column.
 * If no GUID is given, one is generated automatically.
 *
 * <p>German: <em>Ergebnis der Belegrecherche</em>
 */
public final class DocumentProviderResult {

    private final List<SourceDocument> documents;
    private final String guid;
    private final FindStatus status;
    private final String message;

    private DocumentProviderResult(List<SourceDocument> documents, String guid,
                                   FindStatus status, String message) {
        this.documents = List.copyOf(documents);
        this.guid      = guid;
        this.status    = status;
        this.message   = message;
    }

    // ---- factory methods ----

    /**
     * Result with no external GUID — the processor will generate one.
     * Status is inferred: {@link FindStatus#FOUND} for a non-empty list,
     * {@link FindStatus#NOT_FOUND} for an empty list.
     */
    public static DocumentProviderResult of(List<SourceDocument> documents) {
        FindStatus status = documents.isEmpty() ? FindStatus.NOT_FOUND : FindStatus.FOUND;
        return new DocumentProviderResult(documents, null, status, null);
    }

    /**
     * Result with an externally provided GUID.
     * Status is inferred: {@link FindStatus#FOUND} for a non-empty list,
     * {@link FindStatus#NOT_FOUND} for an empty list.
     *
     * @param documents source documents for this booking line
     * @param guid      UUID string in the format used by DATEV (e.g. {@code "AB12CD34-..."});
     *                  must match the pattern {@code [A-F0-9-]{36}} after upper-casing
     */
    public static DocumentProviderResult of(List<SourceDocument> documents, String guid) {
        FindStatus status = documents.isEmpty() ? FindStatus.NOT_FOUND : FindStatus.FOUND;
        return new DocumentProviderResult(documents, guid != null ? guid.toUpperCase() : null,
                status, null);
    }

    /**
     * Result for a booking line that was deliberately skipped (e.g. no document link present).
     *
     * @param message optional description of why the line was skipped; may be {@code null}
     */
    public static DocumentProviderResult skipped(String message) {
        return new DocumentProviderResult(List.of(), null, FindStatus.SKIPPED, message);
    }

    /**
     * Result for a booking line where a search was performed but no document was found.
     *
     * @param message optional description of what was searched; may be {@code null}
     */
    public static DocumentProviderResult notFound(String message) {
        return new DocumentProviderResult(List.of(), null, FindStatus.NOT_FOUND, message);
    }

    /**
     * Returns a copy of this result with the given message attached.
     * Useful for adding detail to a result created by {@link #of}.
     */
    public DocumentProviderResult withMessage(String message) {
        return new DocumentProviderResult(documents, guid, status, message);
    }

    // ---- accessors ----

    /** The resolved source documents. Never null, may be empty. */
    public List<SourceDocument> getDocuments() {
        return documents;
    }

    /**
     * The external GUID for this document group, if provided by the plugin.
     * Empty means the processor will generate a random UUID.
     */
    public Optional<String> getGuid() {
        return Optional.ofNullable(guid);
    }

    /** The outcome of the document lookup. Never null. */
    public FindStatus getStatus() {
        return status;
    }

    /**
     * An optional human-readable message describing the lookup result in more detail.
     * Intended for verbose/diagnostic output. Empty if the provider did not supply one.
     */
    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }
}
