package de.bewidata.datev.xmlonline.split;

import de.bewidata.datev.xmlonline.model.BookingLineWithDocuments;
import de.bewidata.datev.xmlonline.model.SourceDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits booking lines by the DATEV document category (Belegkreis) of their documents:
 * <ol>
 *   <li>Group 1: incoming invoices (Rechnungseingang, documentType = 1)</li>
 *   <li>Group 2: outgoing invoices (Rechnungsausgang, documentType = 2)</li>
 *   <li>Group 3: mixed / no documents</li>
 * </ol>
 * The grouping is determined by the document type of the first document.
 * Lines without documents are included in group 3 (they appear in the CSV output
 * but produce no {@code <document>} entry in document.xml).
 *
 * <p>German: <em>NachBelegtypSplitStrategie</em>
 */
public class ByDocumentTypeSplitStrategy implements BatchSplitStrategy {

    @Override
    public List<List<BookingLineWithDocuments>> split(List<BookingLineWithDocuments> lines) {
        List<BookingLineWithDocuments> incoming = new ArrayList<>();
        List<BookingLineWithDocuments> outgoing = new ArrayList<>();
        List<BookingLineWithDocuments> others   = new ArrayList<>();

        for (BookingLineWithDocuments lwd : lines) {
            switch (firstDocumentType(lwd)) {
                case SourceDocument.DOCUMENT_TYPE_INCOMING -> incoming.add(lwd);
                case SourceDocument.DOCUMENT_TYPE_OUTGOING -> outgoing.add(lwd);
                default                                   -> others.add(lwd);
            }
        }

        List<List<BookingLineWithDocuments>> groups = new ArrayList<>();
        if (!incoming.isEmpty()) groups.add(incoming);
        if (!outgoing.isEmpty()) groups.add(outgoing);

        // Lines without documents are merged into the first typed group rather than
        // forming a separate ZIP — they have no Belegkreis and belong alongside the
        // other booking lines. If no typed group exists they form the sole group.
        if (!others.isEmpty()) {
            if (!groups.isEmpty()) {
                groups.get(0).addAll(others);
            } else {
                groups.add(others);
            }
        }
        return groups;
    }

    private byte firstDocumentType(BookingLineWithDocuments lwd) {
        return lwd.documents().isEmpty() ? 0 : lwd.documents().get(0).getDocumentType();
    }
}
