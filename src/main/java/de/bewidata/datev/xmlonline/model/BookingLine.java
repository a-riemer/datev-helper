package de.bewidata.datev.xmlonline.model;

import de.bewidata.datev.xmlonline.extf.ExtfColumn;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single booking line (transaction) from a DATEV EXTF booking batch.
 *
 * <p>All column values are stored as a map (column name → value) so that
 * {@link de.bewidata.datev.xmlonline.plugin.SourceDocumentProvider} implementations
 * can access any field by its DATEV column name via {@link #field(String)}.
 * Use {@link ExtfColumn} constants as keys — never hardcode the German column names.
 *
 * <p>German accounting term: <em>Buchungszeile</em>
 */
public final class BookingLine {

    /**
     * DATEV EXTF column name for the document link.
     * Value format: {@code BEDI"<UUID>"} — the UUID part is enclosed in double quotes.
     *
     * @deprecated Use {@link ExtfColumn#DOCUMENT_LINK} instead.
     */
    @Deprecated
    public static final String COLUMN_DOCUMENT_LINK = ExtfColumn.DOCUMENT_LINK;

    private final int lineNumber;
    private final Map<String, String> fields;
    private String documentLink;

    public BookingLine(int lineNumber, Map<String, String> fields) {
        this.lineNumber  = lineNumber;
        this.fields      = new LinkedHashMap<>(fields);
        this.documentLink = fields.getOrDefault(ExtfColumn.DOCUMENT_LINK, "");
    }

    /** 1-based line number within the data rows (excluding the two EXTF header rows). */
    public int getLineNumber() { return lineNumber; }

    /** Read-only view of all column values keyed by DATEV column name. */
    public Map<String, String> getFields() { return Collections.unmodifiableMap(fields); }

    /**
     * Returns the value of the given column, or an empty string if absent.
     * Use {@link ExtfColumn} constants as the key.
     */
    public String field(String columnName) {
        return fields.getOrDefault(columnName, "");
    }

    // -------------------------------------------------------------------------
    // Convenience accessors — grouped as in ExtfColumn
    // -------------------------------------------------------------------------

    /** Pos. 1 — Umsatz (ohne Soll/Haben-Kz) */
    public String getAmount()               { return field(ExtfColumn.AMOUNT); }

    /** Pos. 2 — Soll/Haben-Kennzeichen ("S" or "H") */
    public String getDebitCreditIndicator() { return field(ExtfColumn.DEBIT_CREDIT_INDICATOR); }

    /** Pos. 3 — WKZ Umsatz */
    public String getCurrencyCode()         { return field(ExtfColumn.CURRENCY_CODE); }

    /** Pos. 7 — Konto */
    public String getAccount()              { return field(ExtfColumn.ACCOUNT); }

    /** Pos. 8 — Gegenkonto (ohne BU-Schlüssel) */
    public String getCounterAccount()       { return field(ExtfColumn.COUNTER_ACCOUNT); }

    /** Pos. 9 — BU-Schlüssel */
    public String getBuKey()                { return field(ExtfColumn.BU_KEY); }

    /** Pos. 10 — Belegdatum */
    public String getDocumentDate()         { return field(ExtfColumn.DOCUMENT_DATE); }

    /** Pos. 11 — Belegfeld 1 (usually the invoice number) */
    public String getDocumentField1()       { return field(ExtfColumn.DOCUMENT_FIELD1); }

    /** Pos. 12 — Belegfeld 2 */
    public String getDocumentField2()       { return field(ExtfColumn.DOCUMENT_FIELD2); }

    /** Pos. 13 — Skonto */
    public String getCashDiscount()         { return field(ExtfColumn.CASH_DISCOUNT); }

    /** Pos. 14 — Buchungstext */
    public String getBookingText()          { return field(ExtfColumn.BOOKING_TEXT); }

    /** Pos. 20 — Beleglink (BEDI + UUID, set by this library) */
    public String getDocumentLink()         { return documentLink; }

    /** Pos. 37 — KOST1 - Kostenstelle */
    public String getCostCentre1()          { return field(ExtfColumn.COST_CENTRE_1); }

    /** Pos. 38 — KOST2 - Kostenstelle */
    public String getCostCentre2()          { return field(ExtfColumn.COST_CENTRE_2); }

    /** Pos. 95 — Auftragsnummer */
    public String getOrderNumber()          { return field(ExtfColumn.ORDER_NUMBER); }

    /** Pos. 103 — Buchungs GUID */
    public String getBookingGuid()          { return field(ExtfColumn.BOOKING_GUID); }

    /** Pos. 115 — Leistungsdatum */
    public String getServiceDate()          { return field(ExtfColumn.SERVICE_DATE); }

    /** Pos. 117 — Fälligkeit */
    public String getDueDate()              { return field(ExtfColumn.DUE_DATE); }

    /**
     * Sets the BEDI document link for this line.
     * Called by {@link de.bewidata.datev.xmlonline.xml.DocumentXmlBuilder} as a side effect.
     */
    public void setDocumentLink(String bediLink) {
        this.documentLink = bediLink;
        this.fields.put(ExtfColumn.DOCUMENT_LINK, bediLink);
    }
}
