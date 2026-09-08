package de.bewidata.datev.xmlonline.extf;

import de.bewidata.datev.xmlonline.model.BookingBatch;
import de.bewidata.datev.xmlonline.model.BookingLine;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a DATEV booking batch in EXTF format.
 *
 * <p>EXTF file layout:
 * <ul>
 *   <li>Row 1: metadata ({@code "EXTF";700;21;"Buchungsstapel";...})</li>
 *   <li>Row 2: column headers</li>
 *   <li>Rows 3+: booking data</li>
 * </ul>
 * Delimiter: semicolon; quote character: double quote.
 *
 * <p>DATEV EXTF files use <b>Windows-1252</b> encoding by default.
 * Use {@link #parse(Path, Charset)} to override.
 */
public class ExtfParser {

    private static final char DELIMITER = ';';

    /**
     * The encoding used by DATEV for EXTF export files.
     * Use this constant when writing or comparing EXTF content outside this class.
     */
    public static final Charset DATEV_CHARSET = Charset.forName("windows-1252");

    /** Parses the file using the standard DATEV Windows-1252 encoding. */
    public BookingBatch parse(Path file) throws IOException {
        return parse(file, DATEV_CHARSET);
    }

    public BookingBatch parse(Path file, Charset charset) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            return parse(file.getFileName().toString(), reader);
        } catch (MalformedInputException e) {
            throw new IOException(String.format(
                "Cannot read '%s' using %s encoding: the file contains a byte sequence " +
                "that is not valid in this encoding (byte length %d). " +
                "DATEV EXTF files normally use Windows-1252 — " +
                "try parse(path, Charset.forName(\"windows-1252\")).",
                file.getFileName(), charset.displayName(), e.getInputLength()), e);
        }
    }

    /** Parses from a {@link Reader} without a file name in error messages. */
    public BookingBatch parse(Reader reader) throws IOException {
        return parse("<stream>", reader instanceof BufferedReader br ? br : new BufferedReader(reader));
    }

    // Positions (0-based) of relevant fields in the EXTF metadata row (row 1).
    // Source: DATEV EXTF Buchungsstapel format specification.
    private static final int HEADER_IDX_CONSULTANT_NUMBER = 10; // Beraternummer
    private static final int HEADER_IDX_CLIENT_NUMBER     = 11; // Mandantennummer
    private static final int HEADER_IDX_DESCRIPTION       = 16; // Bezeichnung

    private BookingBatch parse(String sourceName, BufferedReader buffered) throws IOException {
        // Row 1: EXTF metadata line — read raw, preserved verbatim
        String extfHeaderLine = buffered.readLine();
        if (extfHeaderLine == null) {
            throw new IOException(
                "'" + sourceName + "' is empty — expected an EXTF file with at least two header rows.");
        }
        if (!extfHeaderLine.startsWith("\"EXTF\"") && !extfHeaderLine.startsWith("EXTF")) {
            throw new IOException(String.format(
                "'%s' does not appear to be an EXTF file: row 1 must start with \"EXTF\" " +
                "but was: %s",
                sourceName, truncate(extfHeaderLine, 80)));
        }

        // Parse the well-known fields from the EXTF metadata row.
        Long   consultantNumber = null;
        Long   clientNumber     = null;
        String description      = "";
        try {
            CSVFormat fmt = CSVFormat.DEFAULT.withDelimiter(DELIMITER).withQuote('"');
            try (CSVParser hp = CSVParser.parse(extfHeaderLine, fmt)) {
                List<CSVRecord> recs = hp.getRecords();
                if (!recs.isEmpty()) {
                    CSVRecord r = recs.get(0);
                    consultantNumber = parseLong(r,   HEADER_IDX_CONSULTANT_NUMBER);
                    clientNumber     = parseLong(r,   HEADER_IDX_CLIENT_NUMBER);
                    description      = parseString(r, HEADER_IDX_DESCRIPTION);
                }
            }
        } catch (Exception ignored) {
            // Never fail the parse because of metadata extraction issues.
        }

        // Row 2: column headers — read and validated manually so we can report the exact
        // position of any missing or empty header name (Commons CSV only shows the full row).
        String headerRow = buffered.readLine();
        if (headerRow == null || headerRow.isBlank()) {
            throw new IOException(
                "'" + sourceName + "': row 2 (column headers) is missing or empty.");
        }
        List<String> columnNames = parseHeaderRow(sourceName, headerRow);

        // Rows 3+: booking data — parsed with explicit column names (header already consumed)
        CSVFormat format = CSVFormat.DEFAULT
                .withDelimiter(DELIMITER)
                .withQuote('"')
                .withHeader(columnNames.toArray(new String[0]))
                .withIgnoreEmptyLines(false);

        List<BookingLine> lines = new ArrayList<>();
        try (CSVParser parser = format.parse(buffered)) {
            for (CSVRecord record : parser) {
                Map<String, String> fields = new LinkedHashMap<>();
                for (String column : columnNames) {
                    fields.put(column, record.get(column));
                }
                lines.add(new BookingLine((int) record.getRecordNumber(), fields));
            }
        } catch (IOException e) {
            if (e.getMessage() == null || !e.getMessage().contains(sourceName)) {
                throw new IOException(
                    "Error reading CSV data from '" + sourceName + "': " + e.getMessage(), e);
            }
            throw e;
        }

        return new BookingBatch(extfHeaderLine, columnNames, lines,
                consultantNumber, clientNumber, description);
    }

    /**
     * Parses the header row and validates that every column name is non-empty.
     * Reports the exact 1-based column position if a name is missing.
     */
    private List<String> parseHeaderRow(String sourceName, String headerRow) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.withDelimiter(DELIMITER).withQuote('"');
        List<CSVRecord> records;
        try (CSVParser parser = CSVParser.parse(headerRow, format)) {
            records = parser.getRecords();
        }

        if (records.isEmpty()) {
            throw new IOException("'" + sourceName + "': row 2 (column headers) is empty.");
        }

        CSVRecord record = records.get(0);

        // Collect raw values, then strip trailing empty entries.
        // A trailing semicolon in DATEV EXTF exports creates a spurious empty last field.
        List<String> raw = new ArrayList<>(record.size());
        for (int i = 0; i < record.size(); i++) {
            raw.add(record.get(i));
        }
        while (!raw.isEmpty() && raw.get(raw.size() - 1).isBlank()) {
            raw.remove(raw.size() - 1);
        }

        // Validate: no empty name may remain in the middle of the header
        List<String> names = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            String name = raw.get(i);
            if (name.isBlank()) {
                String expected = i < ExtfColumn.ALL_COLUMNS.size()
                    ? " Expected column name at this position: \"" + ExtfColumn.ALL_COLUMNS.get(i) + "\"."
                    : "";
                throw new IOException(String.format(
                    "'%s': column header at position %d (1-based) is empty.%s " +
                    "Header row: %s",
                    sourceName, i + 1, expected, truncate(headerRow, 200)));
            }
            names.add(name);
        }
        return names;
    }

    private static Long parseLong(CSVRecord r, int index) {
        if (r.size() <= index) return null;
        String s = r.get(index).strip();
        if (s.isEmpty()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }

    private static String parseString(CSVRecord r, int index) {
        if (r.size() <= index) return "";
        return r.get(index).strip();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
