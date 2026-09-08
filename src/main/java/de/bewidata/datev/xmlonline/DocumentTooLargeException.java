package de.bewidata.datev.xmlonline;

import java.nio.file.Path;

/**
 * Thrown when a single document exceeds the maximum allowed file size.
 *
 * <p>DATEV limit: 20 MB per document (configurable via {@link DATEVXmlConfig}).
 *
 * <p>German: <em>DokumentZuGrossException</em>
 */
public class DocumentTooLargeException extends Exception {

    private final Path path;
    private final long size;
    private final long limit;

    public DocumentTooLargeException(Path path, long size, long limit) {
        super(String.format("Document '%s' (%,d bytes) exceeds the limit of %,d bytes.",
                path.getFileName(), size, limit));
        this.path  = path;
        this.size  = size;
        this.limit = limit;
    }

    public Path getPath()  { return path; }
    public long getSize()  { return size; }
    public long getLimit() { return limit; }
}
