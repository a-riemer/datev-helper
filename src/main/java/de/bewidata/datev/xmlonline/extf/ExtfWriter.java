package de.bewidata.datev.xmlonline.extf;

import de.bewidata.datev.xmlonline.model.BookingBatch;
import de.bewidata.datev.xmlonline.model.BookingLine;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a (partial) booking batch back to an EXTF-formatted CSV file.
 *
 * <p>Quoting rules (per DATEV EXTF Buchungsstapel specification):
 * <ul>
 *   <li>All non-empty values — including column header names (row 2) and all booking data
 *       fields — are enclosed in double quotes.</li>
 *   <li>Empty fields are written without quotes (just the delimiter).</li>
 *   <li>Double quotes inside a value are escaped as {@code ""}.</li>
 * </ul>
 */
public class ExtfWriter {

    private static final char DELIMITER = ';';

    public void write(BookingBatch batch, List<BookingLine> lines, Path targetFile)
            throws IOException {
        write(batch, lines, targetFile, ExtfParser.DATEV_CHARSET);
    }

    public void write(BookingBatch batch, List<BookingLine> lines, Path targetFile, Charset charset)
            throws IOException {
        try (Writer writer = Files.newBufferedWriter(targetFile, charset)) {
            write(batch, lines, writer);
        }
    }

    public void write(BookingBatch batch, List<BookingLine> lines, Writer writer)
            throws IOException {
        // Row 1: EXTF metadata — written verbatim
        writer.write(batch.getExtfHeaderLine());
        writer.write("\r\n");

        List<String> columns = columnsWithDocumentLink(batch);

        // Row 2: column headers — always quoted (they are text identifiers)
        writer.write(buildHeaderRow(columns));

        // Rows 3+: booking data with selective quoting
        for (BookingLine line : lines) {
            writer.write(buildDataRow(columns, line));
        }
    }

    // ---- row builders ----

    private String buildHeaderRow(List<String> columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(DELIMITER);
            sb.append(quoted(columns.get(i)));
        }
        sb.append("\r\n");
        return sb.toString();
    }

    private String buildDataRow(List<String> columns, BookingLine line) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(DELIMITER);
            String value = line.getFields().getOrDefault(columns.get(i), "");
            if (!value.isEmpty()) {
                sb.append(quoted(value));
            }
        }
        sb.append("\r\n");
        return sb.toString();
    }

    // ---- helpers ----

    /** Wraps a value in double quotes and escapes any internal double quotes as "". */
    private static String quoted(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /** Returns the column list, appending the document-link column if not already present. */
    private List<String> columnsWithDocumentLink(BookingBatch batch) {
        List<String> columns = new ArrayList<>(batch.getColumnNames());
        if (!columns.contains(ExtfColumn.DOCUMENT_LINK)) {
            columns.add(ExtfColumn.DOCUMENT_LINK);
        }
        return columns;
    }
}
