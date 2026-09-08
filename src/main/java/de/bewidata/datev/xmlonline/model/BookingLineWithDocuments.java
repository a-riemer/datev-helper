package de.bewidata.datev.xmlonline.model;

import de.bewidata.datev.xmlonline.plugin.FindStatus;

import java.util.List;
import java.util.Optional;

/**
 * Pairs a booking line with the source documents resolved for it, the find status,
 * and an optional message from the provider.
 *
 * <p>The {@code externalGuid} is provided by the {@link de.bewidata.datev.xmlonline.plugin.SourceDocumentProvider}
 * and, when present, is used verbatim as the DATEV document GUID instead of a generated one.
 *
 * <p>German: <em>Buchungszeile mit Belegen</em>
 */
public record BookingLineWithDocuments(
        BookingLine line,
        List<SourceDocument> documents,
        String externalGuid,
        FindStatus findStatus,
        String findMessage) {

    /** Convenience constructor without an external GUID or status (generates a GUID, status NOT_FOUND). */
    public BookingLineWithDocuments(BookingLine line, List<SourceDocument> documents) {
        this(line, documents, null, FindStatus.NOT_FOUND, null);
    }

    /** Total file size of all attached documents in bytes. */
    public long totalSize() {
        return documents.stream().mapToLong(SourceDocument::fileSize).sum();
    }

    public boolean hasDocuments() {
        return !documents.isEmpty();
    }

    /** The externally provided GUID, if any. Empty means the processor generates one. */
    public Optional<String> getExternalGuid() {
        return Optional.ofNullable(externalGuid);
    }

    /** The optional message from the provider describing the lookup outcome. */
    public Optional<String> getFindMessage() {
        return Optional.ofNullable(findMessage);
    }
}
