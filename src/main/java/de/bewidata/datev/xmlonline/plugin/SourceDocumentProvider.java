package de.bewidata.datev.xmlonline.plugin;

import de.bewidata.datev.xmlonline.model.BookingLine;
import de.bewidata.datev.xmlonline.model.SourceDocument;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plugin interface for resolving source documents for booking lines.
 *
 * <p>Implementations look up physical document files (e.g. PDFs from a DMS)
 * for a given booking line. The implementation is application-specific and
 * provided by the caller.
 *
 * <p>The result may optionally include a GUID. When present, that GUID is used
 * as the {@code guid} attribute in {@code document.xml} and as the {@code BEDI+}
 * value in the CSV {@code Beleglink} column. If absent, a random UUID is generated.
 *
 * <p>German: <em>Belegrecherche</em>
 *
 * <p>Example for MyApp-DMS:
 * <pre>
 * public class MyAppDocumentProvider implements SourceDocumentProvider {
 *     public DocumentProviderResult findDocuments(BookingLine line) {
 *         DmsEntry entry = dms.fetch(line.getDocumentField1());
 *         if (entry == null) return DocumentProviderResult.of(List.of());
 *         SourceDocument doc = SourceDocument.incoming(entry.path()).build();
 *         // Reuse the DMS UUID so the Beleglink stays stable across re-imports
 *         return DocumentProviderResult.of(List.of(doc), entry.uuid());
 *     }
 * }
 * </pre>
 */
@FunctionalInterface
public interface SourceDocumentProvider {

    /**
     * Finds source documents for the given booking line.
     *
     * @param line the booking line to look up documents for
     * @return result containing the documents (may be empty) and an optional GUID
     * @throws SourceDocumentException on technical lookup failures
     */
    DocumentProviderResult findDocuments(BookingLine line) throws SourceDocumentException;

    /**
     * Called once after instantiation to pass configuration parameters.
     *
     * <p>Providers that require configuration (e.g. a directory path or connection URL)
     * must override this method. The default implementation is a no-op, so existing
     * lambda-style providers continue to work without changes.
     *
     * <p>When the CLI loads a provider via {@code --provider-class}, any
     * {@code --provider-param key=value} arguments are collected and passed here.
     *
     * @param params key/value pairs supplied by the caller; never {@code null}
     * @throws Exception if a required parameter is missing or invalid
     */
    default void configure(Map<String, String> params) throws Exception {
        // no-op – providers that need configuration must override this
    }

    /**
     * Returns a short human-readable summary of the provider's active configuration,
     * intended for CLI startup output. The default implementation returns empty.
     *
     * <p>Called by the CLI after {@link #configure} to inform the user where the
     * provider will look for documents.
     */
    default Optional<String> configSummary() {
        return Optional.empty();
    }
}
