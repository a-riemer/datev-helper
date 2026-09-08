package de.bewidata.datev.xmlonline.plugin;

/**
 * Thrown when the source document lookup fails for a booking line.
 *
 * <p>German: <em>BelegRechercheException</em>
 */
public class SourceDocumentException extends Exception {

    public SourceDocumentException(String message) {
        super(message);
    }

    public SourceDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
