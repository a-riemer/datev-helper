package de.bewidata.datev.xmlonline.split;

import de.bewidata.datev.xmlonline.model.BookingLineWithDocuments;

import java.util.List;

/**
 * Strategy for logically grouping booking lines into batches.
 *
 * <p>Each group becomes the basis for one or more ZIP archives.
 * The {@code ZipArchiveBuilder} may split a group further if it exceeds
 * the maximum ZIP size (100 MB by default).
 *
 * <p>Implementations decide by business rules (e.g. by document type).
 * Size constraints are handled downstream.
 *
 * <p>German: <em>StapelSplitStrategie</em>
 */
public interface BatchSplitStrategy {

    /**
     * Splits the booking lines into logical groups.
     *
     * @param lines all booking lines with their resolved documents
     * @return groups; each group produces at least one ZIP archive
     */
    List<List<BookingLineWithDocuments>> split(List<BookingLineWithDocuments> lines);
}
