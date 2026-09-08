package de.bewidata.datev.xmlonline.plugin;

/**
 * Outcome of a {@link SourceDocumentProvider#findDocuments} call for a single booking line.
 *
 * <p>German: <em>Recherchestatus</em>
 */
public enum FindStatus {

    /** At least one document was found and returned. */
    FOUND,

    /** A search was performed but no matching document was found. */
    NOT_FOUND,

    /** The provider deliberately skipped the lookup (e.g. no document link present). */
    SKIPPED
}
