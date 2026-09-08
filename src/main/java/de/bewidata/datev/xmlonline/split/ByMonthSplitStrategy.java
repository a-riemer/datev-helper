package de.bewidata.datev.xmlonline.split;

import de.bewidata.datev.xmlonline.model.BookingLineWithDocuments;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Splits booking lines by accounting month, derived from each line's {@code Belegdatum}.
 *
 * <p>DATEV stores the {@code Belegdatum} in one of two formats:
 * <ul>
 *   <li>{@code DDMMYYYY} (8 digits) — year and month are read directly</li>
 *   <li>{@code DDMM} (4 digits) — year is taken from {@code fallbackYear}
 *       (defaults to the current calendar year)</li>
 * </ul>
 *
 * <p>Lines with a blank or unparseable {@code Belegdatum} are merged into the first
 * group that has a parseable date. If no line has a parseable date, all lines form
 * a single group.
 *
 * <p>Groups are returned in ascending chronological order ({@code YYYY-MM}).
 *
 * <p>The static helper {@link #extractYearMonth(String, int)} is also used by
 * {@link de.bewidata.datev.acsapi.DATEVApiProcessor} to derive the {@code register}
 * metadata field for each group's document upload.
 *
 * <p>German: <em>NachMonatSplitStrategie</em>
 */
public class ByMonthSplitStrategy implements BatchSplitStrategy {

    private final int fallbackYear;

    /**
     * Creates a strategy that uses the current calendar year as the fallback year
     * for {@code Belegdatum} values in 4-digit {@code DDMM} format.
     */
    public ByMonthSplitStrategy() {
        this(LocalDate.now().getYear());
    }

    /**
     * Creates a strategy with an explicit fallback year for 4-digit {@code DDMM} dates.
     *
     * @param fallbackYear year used when {@code Belegdatum} is in {@code DDMM} format
     *                     (typically the fiscal year of the booking batch)
     */
    public ByMonthSplitStrategy(int fallbackYear) {
        this.fallbackYear = fallbackYear;
    }

    @Override
    public List<List<BookingLineWithDocuments>> split(List<BookingLineWithDocuments> lines) {
        LinkedHashMap<String, List<BookingLineWithDocuments>> byMonth = new LinkedHashMap<>();
        List<BookingLineWithDocuments> undated = new ArrayList<>();

        for (BookingLineWithDocuments lwd : lines) {
            String key = extractYearMonth(lwd.line().getDocumentDate(), fallbackYear);
            if (key != null) {
                byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(lwd);
            } else {
                undated.add(lwd);
            }
        }

        // Sort groups chronologically by their YYYY-MM key.
        List<List<BookingLineWithDocuments>> groups = byMonth.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(java.util.Map.Entry::getValue)
                .collect(Collectors.toCollection(ArrayList::new));

        // Undated lines → merge into the first group; if no dated group exists, form a sole group.
        if (!undated.isEmpty()) {
            if (!groups.isEmpty()) {
                groups.get(0).addAll(undated);
            } else {
                groups.add(undated);
            }
        }

        return groups;
    }

    /**
     * Derives an accounting period string ({@code YYYY-MM}) from a DATEV {@code Belegdatum} value.
     *
     * <p>Recognised formats:
     * <ul>
     *   <li>{@code DDMMYYYY} (8 chars) — month and year read directly from the string</li>
     *   <li>{@code DDMM} (4 chars) — month read from the string; year from {@code fallbackYear}</li>
     * </ul>
     *
     * @param belegdatum  raw value from the {@code Belegdatum} column
     * @param fallbackYear year applied to 4-digit {@code DDMM} dates
     * @return {@code "YYYY-MM"} string, or {@code null} if the value is blank or unparseable
     */
    public static String extractYearMonth(String belegdatum, int fallbackYear) {
        if (belegdatum == null || belegdatum.isBlank()) return null;
        String s = belegdatum.strip();
        try {
            if (s.length() == 8) {
                // DDMMYYYY
                int month = Integer.parseInt(s.substring(2, 4));
                int year  = Integer.parseInt(s.substring(4, 8));
                if (month >= 1 && month <= 12 && year >= 1900) {
                    return String.format("%04d-%02d", year, month);
                }
            } else if (s.length() == 4) {
                // DDMM — use fallback year
                int month = Integer.parseInt(s.substring(2, 4));
                if (month >= 1 && month <= 12) {
                    return String.format("%04d-%02d", fallbackYear, month);
                }
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }
}
