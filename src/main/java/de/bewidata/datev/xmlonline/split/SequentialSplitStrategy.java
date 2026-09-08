package de.bewidata.datev.xmlonline.split;

import de.bewidata.datev.xmlonline.model.BookingLineWithDocuments;

import java.util.ArrayList;
import java.util.List;

/**
 * No logical pre-grouping — all lines are returned as a single group.
 * The {@code ZipArchiveBuilder} handles size-based splitting (100 MB limit).
 *
 * <p>German: <em>SequenziellSplitStrategie</em>
 */
public class SequentialSplitStrategy implements BatchSplitStrategy {

    @Override
    public List<List<BookingLineWithDocuments>> split(List<BookingLineWithDocuments> lines) {
        return List.of(new ArrayList<>(lines));
    }
}
