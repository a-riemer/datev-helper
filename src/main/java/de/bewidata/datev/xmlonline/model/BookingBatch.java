package de.bewidata.datev.xmlonline.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A complete DATEV booking batch in EXTF format.
 *
 * <p>Holds the raw EXTF metadata line (row 1), the column names (row 2),
 * and all booking lines (rows 3+).
 *
 * <p>Row 1 of every EXTF file also carries the Beraternummer (consultant number),
 * Mandantennummer (client number), and the batch description (Bezeichnung).
 * These are parsed and exposed as typed fields so callers can use them as fallback
 * values when those details are not known from another source.
 *
 * <p>Note: the client name (Mandantenname) is <em>not</em> part of the EXTF header
 * and is therefore not available here.
 *
 * <p>German accounting term: <em>Buchungsstapel</em>
 */
public final class BookingBatch {

    private final String extfHeaderLine;
    private final List<String> columnNames;
    private final List<BookingLine> lines;

    /** Beraternummer from the EXTF row 1, or {@code null} if absent / not parseable. */
    private final Long consultantNumber;

    /** Mandantennummer from the EXTF row 1, or {@code null} if absent / not parseable. */
    private final Long clientNumber;

    /** Bezeichnung (batch label) from the EXTF row 1; empty string if absent. */
    private final String description;

    public BookingBatch(String extfHeaderLine,
                        List<String> columnNames,
                        List<BookingLine> lines,
                        Long consultantNumber,
                        Long clientNumber,
                        String description) {
        this.extfHeaderLine  = extfHeaderLine;
        this.columnNames     = Collections.unmodifiableList(new ArrayList<>(columnNames));
        this.lines           = new ArrayList<>(lines);
        this.consultantNumber = consultantNumber;
        this.clientNumber    = clientNumber;
        this.description     = description != null ? description : "";
    }

    /** Row 1 of the EXTF file (raw string, preserved verbatim on output). */
    public String getExtfHeaderLine()    { return extfHeaderLine; }

    /** Column names from row 2 of the EXTF file. */
    public List<String> getColumnNames() { return columnNames; }

    /** All booking lines (starting from row 3). */
    public List<BookingLine> getLines()  { return lines; }

    public boolean hasColumn(String name) { return columnNames.contains(name); }

    /**
     * Beraternummer as read from the EXTF header row.
     * Returns {@code null} if the field was absent or could not be parsed as a number.
     */
    public Long getConsultantNumber()    { return consultantNumber; }

    /**
     * Mandantennummer as read from the EXTF header row.
     * Returns {@code null} if the field was absent or could not be parsed as a number.
     */
    public Long getClientNumber()        { return clientNumber; }

    /**
     * Bezeichnung (batch label) from the EXTF header row.
     * Returns an empty string if the field was absent.
     */
    public String getDescription()       { return description; }
}
