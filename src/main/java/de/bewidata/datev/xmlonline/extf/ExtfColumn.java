package de.bewidata.datev.xmlonline.extf;

import java.util.List;
import java.util.Set;

/**
 * Column name constants for the DATEV EXTF Buchungsstapel format (version 9, 125 columns).
 *
 * <p>All string values are the exact German column header names as specified by DATEV and
 * as they appear in row 2 of an EXTF CSV file. They must not be translated or changed,
 * since {@link de.bewidata.datev.xmlonline.model.BookingLine#field(String)} uses them as
 * map keys.
 *
 * <p><b>IMPORTANT:</b> Verify these names against your copy of the official DATEV documentation
 * "DATEV Buchungsdatenservice – Beschreibung der EXTF-Schnittstelle" for the exact format
 * version you are working with. Column names and order may differ between versions.
 *
 * <p>Source: DATEV EXTF CSV-Schnittstelle, Formatversion 9 (Buchungsstapel, Kennzeichen 21).
 */
public final class ExtfColumn {

    private ExtfColumn() {}

    // -------------------------------------------------------------------------
    // Columns 1–10: Core booking fields (Pflichtfelder)
    // -------------------------------------------------------------------------

    /** Pos. 1 — Umsatz (ohne Soll/Haben-Kz) */
    public static final String AMOUNT                   = "Umsatz (ohne Soll/Haben-Kz)";

    /** Pos. 2 — Soll/Haben-Kennzeichen: "S" = Debit, "H" = Credit */
    public static final String DEBIT_CREDIT_INDICATOR   = "Soll/Haben-Kennzeichen";

    /** Pos. 3 — WKZ Umsatz (Währungskennzeichen für den Umsatz) */
    public static final String CURRENCY_CODE            = "WKZ Umsatz";

    /** Pos. 4 — Kurs */
    public static final String EXCHANGE_RATE            = "Kurs";

    /** Pos. 5 — Basisumsatz */
    public static final String BASE_AMOUNT              = "Basisumsatz";

    /** Pos. 6 — WKZ Basisumsatz */
    public static final String BASE_CURRENCY_CODE       = "WKZ Basisumsatz";

    /** Pos. 7 — Konto */
    public static final String ACCOUNT                  = "Konto";

    /** Pos. 8 — Gegenkonto (ohne BU-Schlüssel) */
    public static final String COUNTER_ACCOUNT          = "Gegenkonto (ohne BU-Schlüssel)";

    /** Pos. 9 — BU-Schlüssel */
    public static final String BU_KEY                   = "BU-Schlüssel";

    /** Pos. 10 — Belegdatum (DDMM or DDMMYYYY) */
    public static final String DOCUMENT_DATE            = "Belegdatum";

    // -------------------------------------------------------------------------
    // Columns 11–20: Document reference and misc
    // -------------------------------------------------------------------------

    /** Pos. 11 — Belegfeld 1 (usually invoice number, max 36 chars) */
    public static final String DOCUMENT_FIELD1          = "Belegfeld 1";

    /** Pos. 12 — Belegfeld 2 (max 12 chars) */
    public static final String DOCUMENT_FIELD2          = "Belegfeld 2";

    /** Pos. 13 — Skonto */
    public static final String CASH_DISCOUNT            = "Skonto";

    /** Pos. 14 — Buchungstext (max 60 chars) */
    public static final String BOOKING_TEXT             = "Buchungstext";

    /** Pos. 15 — Postensperre (1 = gesperrt) */
    public static final String ITEM_LOCK                = "Postensperre";

    /** Pos. 16 — Diverse Adressnummer */
    public static final String MISC_ADDRESS_NUMBER      = "Diverse Adressnummer";

    /** Pos. 17 — Geschäftspartnerbank */
    public static final String PARTNER_BANK             = "Geschäftspartnerbank";

    /** Pos. 18 — Sachverhalt */
    public static final String SUBJECT_MATTER           = "Sachverhalt";

    /** Pos. 19 — Zinssperre (1 = gesperrt) */
    public static final String INTEREST_LOCK            = "Zinssperre";

    /** Pos. 20 — Beleglink; format: {@code BEDI"<UUID>"} — the UUID part is enclosed in double
     *  quotes, which in CSV appear as {@code "BEDI ""<UUID>"""}. */
    public static final String DOCUMENT_LINK            = "Beleglink";

    // -------------------------------------------------------------------------
    // Columns 21–36: Beleginfo (8 pairs of Art / Inhalt)
    // -------------------------------------------------------------------------

    public static final String DOCUMENT_INFO_TYPE_1      = "Beleginfo - Art 1";
    public static final String DOCUMENT_INFO_CONTENT_1   = "Beleginfo - Inhalt 1";
    public static final String DOCUMENT_INFO_TYPE_2      = "Beleginfo - Art 2";
    public static final String DOCUMENT_INFO_CONTENT_2   = "Beleginfo - Inhalt 2";
    public static final String DOCUMENT_INFO_TYPE_3      = "Beleginfo - Art 3";
    public static final String DOCUMENT_INFO_CONTENT_3   = "Beleginfo - Inhalt 3";
    public static final String DOCUMENT_INFO_TYPE_4      = "Beleginfo - Art 4";
    public static final String DOCUMENT_INFO_CONTENT_4   = "Beleginfo - Inhalt 4";
    public static final String DOCUMENT_INFO_TYPE_5      = "Beleginfo - Art 5";
    public static final String DOCUMENT_INFO_CONTENT_5   = "Beleginfo - Inhalt 5";
    public static final String DOCUMENT_INFO_TYPE_6      = "Beleginfo - Art 6";
    public static final String DOCUMENT_INFO_CONTENT_6   = "Beleginfo - Inhalt 6";
    public static final String DOCUMENT_INFO_TYPE_7      = "Beleginfo - Art 7";
    public static final String DOCUMENT_INFO_CONTENT_7   = "Beleginfo - Inhalt 7";
    public static final String DOCUMENT_INFO_TYPE_8      = "Beleginfo - Art 8";
    public static final String DOCUMENT_INFO_CONTENT_8   = "Beleginfo - Inhalt 8";

    // -------------------------------------------------------------------------
    // Columns 37–47: Cost centre and EU/special tax fields
    // -------------------------------------------------------------------------

    /** Pos. 37 — KOST1 - Kostenstelle */
    public static final String COST_CENTRE_1            = "KOST1 - Kostenstelle";

    /** Pos. 38 — KOST2 - Kostenstelle */
    public static final String COST_CENTRE_2            = "KOST2 - Kostenstelle";

    /** Pos. 39 — Kost-Menge */
    public static final String COST_QUANTITY            = "Kost-Menge";

    /** Pos. 40 — EU-Land u. UStID */
    public static final String EU_COUNTRY_AND_VAT_ID    = "EU-Land u. UStID";

    /** Pos. 41 — EU-Steuersatz */
    public static final String EU_TAX_RATE              = "EU-Steuersatz";

    /** Pos. 42 — Abw. Versteuerungsart */
    public static final String ALT_TAX_METHOD           = "Abw. Versteuerungsart";

    /** Pos. 43 — Sachverhalt L+L */
    public static final String SUBJECT_MATTER_LL        = "Sachverhalt L+L";

    /** Pos. 44 — Funktionsergänzung L+L */
    public static final String FUNCTION_SUPPLEMENT_LL   = "Funktionsergänzung L+L";

    /** Pos. 45 — BU 49 Hauptfunktiontyp */
    public static final String BU49_MAIN_FUNCTION_TYPE  = "BU 49 Hauptfunktiontyp";

    /** Pos. 46 — BU 49 Hauptfunktionsnummer */
    public static final String BU49_MAIN_FUNCTION_NO    = "BU 49 Hauptfunktionsnummer";

    /** Pos. 47 — BU 49 Funktionsergänzung */
    public static final String BU49_FUNCTION_SUPPLEMENT = "BU 49 Funktionsergänzung";

    // -------------------------------------------------------------------------
    // Columns 48–87: Zusatzinformation (20 pairs of Art / Inhalt)
    // -------------------------------------------------------------------------

    public static final String ADDITIONAL_INFO_TYPE_1   = "Zusatzinformation - Art 1";
    public static final String ADDITIONAL_INFO_CONTENT_1 = "Zusatzinformation - Inhalt 1";
    public static final String ADDITIONAL_INFO_TYPE_2   = "Zusatzinformation - Art 2";
    public static final String ADDITIONAL_INFO_CONTENT_2 = "Zusatzinformation - Inhalt 2";
    public static final String ADDITIONAL_INFO_TYPE_3   = "Zusatzinformation - Art 3";
    public static final String ADDITIONAL_INFO_CONTENT_3 = "Zusatzinformation - Inhalt 3";
    public static final String ADDITIONAL_INFO_TYPE_4   = "Zusatzinformation - Art 4";
    public static final String ADDITIONAL_INFO_CONTENT_4 = "Zusatzinformation - Inhalt 4";
    public static final String ADDITIONAL_INFO_TYPE_5   = "Zusatzinformation - Art 5";
    public static final String ADDITIONAL_INFO_CONTENT_5 = "Zusatzinformation - Inhalt 5";
    public static final String ADDITIONAL_INFO_TYPE_6   = "Zusatzinformation - Art 6";
    public static final String ADDITIONAL_INFO_CONTENT_6 = "Zusatzinformation - Inhalt 6";
    public static final String ADDITIONAL_INFO_TYPE_7   = "Zusatzinformation - Art 7";
    public static final String ADDITIONAL_INFO_CONTENT_7 = "Zusatzinformation - Inhalt 7";
    public static final String ADDITIONAL_INFO_TYPE_8   = "Zusatzinformation - Art 8";
    public static final String ADDITIONAL_INFO_CONTENT_8 = "Zusatzinformation - Inhalt 8";
    public static final String ADDITIONAL_INFO_TYPE_9   = "Zusatzinformation - Art 9";
    public static final String ADDITIONAL_INFO_CONTENT_9 = "Zusatzinformation - Inhalt 9";
    public static final String ADDITIONAL_INFO_TYPE_10  = "Zusatzinformation - Art 10";
    public static final String ADDITIONAL_INFO_CONTENT_10 = "Zusatzinformation - Inhalt 10";
    public static final String ADDITIONAL_INFO_TYPE_11  = "Zusatzinformation - Art 11";
    public static final String ADDITIONAL_INFO_CONTENT_11 = "Zusatzinformation - Inhalt 11";
    public static final String ADDITIONAL_INFO_TYPE_12  = "Zusatzinformation - Art 12";
    public static final String ADDITIONAL_INFO_CONTENT_12 = "Zusatzinformation - Inhalt 12";
    public static final String ADDITIONAL_INFO_TYPE_13  = "Zusatzinformation - Art 13";
    public static final String ADDITIONAL_INFO_CONTENT_13 = "Zusatzinformation - Inhalt 13";
    public static final String ADDITIONAL_INFO_TYPE_14  = "Zusatzinformation - Art 14";
    public static final String ADDITIONAL_INFO_CONTENT_14 = "Zusatzinformation - Inhalt 14";
    public static final String ADDITIONAL_INFO_TYPE_15  = "Zusatzinformation - Art 15";
    public static final String ADDITIONAL_INFO_CONTENT_15 = "Zusatzinformation - Inhalt 15";
    public static final String ADDITIONAL_INFO_TYPE_16  = "Zusatzinformation - Art 16";
    public static final String ADDITIONAL_INFO_CONTENT_16 = "Zusatzinformation - Inhalt 16";
    public static final String ADDITIONAL_INFO_TYPE_17  = "Zusatzinformation - Art 17";
    public static final String ADDITIONAL_INFO_CONTENT_17 = "Zusatzinformation - Inhalt 17";
    public static final String ADDITIONAL_INFO_TYPE_18  = "Zusatzinformation - Art 18";
    public static final String ADDITIONAL_INFO_CONTENT_18 = "Zusatzinformation - Inhalt 18";
    public static final String ADDITIONAL_INFO_TYPE_19  = "Zusatzinformation - Art 19";
    public static final String ADDITIONAL_INFO_CONTENT_19 = "Zusatzinformation - Inhalt 19";
    public static final String ADDITIONAL_INFO_TYPE_20  = "Zusatzinformation - Art 20";
    public static final String ADDITIONAL_INFO_CONTENT_20 = "Zusatzinformation - Inhalt 20";

    // -------------------------------------------------------------------------
    // Columns 88–101: Quantity, payment, and advance payment fields
    // -------------------------------------------------------------------------

    /** Pos. 88 — Stück */
    public static final String QUANTITY                 = "Stück";

    /** Pos. 89 — Gewicht */
    public static final String WEIGHT                   = "Gewicht";

    /** Pos. 90 — Zahlweise */
    public static final String PAYMENT_METHOD           = "Zahlweise";

    /** Pos. 91 — Forderungsart */
    public static final String CLAIM_TYPE               = "Forderungsart";

    /** Pos. 92 — Veranlagungsjahr (YYYY) */
    public static final String ASSESSMENT_YEAR          = "Veranlagungsjahr";

    /** Pos. 93 — Zugeordnete Fälligkeit (DDMMYYYY) */
    public static final String ASSIGNED_DUE_DATE        = "Zugeordnete Fälligkeit";

    /** Pos. 94 — Skontotyp */
    public static final String CASH_DISCOUNT_TYPE       = "Skontotyp";

    /** Pos. 95 — Auftragsnummer */
    public static final String ORDER_NUMBER             = "Auftragsnummer";

    /** Pos. 96 — Buchungstyp */
    public static final String BOOKING_TYPE             = "Buchungstyp";

    /** Pos. 97 — USt-Schlüssel (Anzahlungen) */
    public static final String VAT_KEY_ADVANCE          = "USt-Schlüssel (Anzahlungen)";

    /** Pos. 98 — EU-Land (Anzahlungen) */
    public static final String EU_COUNTRY_ADVANCE       = "EU-Land (Anzahlungen)";

    /** Pos. 99 — Sachverhalt L+L (Anzahlungen) */
    public static final String SUBJECT_MATTER_LL_ADVANCE = "Sachverhalt L+L (Anzahlungen)";

    /** Pos. 100 — EU-Steuersatz (Anzahlungen) */
    public static final String EU_TAX_RATE_ADVANCE      = "EU-Steuersatz (Anzahlungen)";

    /** Pos. 101 — Erlöskonto (Anzahlungen) */
    public static final String REVENUE_ACCOUNT_ADVANCE  = "Erlöskonto (Anzahlungen)";

    // -------------------------------------------------------------------------
    // Columns 102–114: Identifiers, locks, and special bookings
    // -------------------------------------------------------------------------

    /** Pos. 102 — Herkunft-Kz */
    public static final String ORIGIN_CODE              = "Herkunft-Kz";

    /** Pos. 103 — Buchungs GUID */
    public static final String BOOKING_GUID             = "Buchungs GUID";

    /** Pos. 104 — KOST-Datum */
    public static final String COST_DATE                = "KOST-Datum";

    /** Pos. 105 — SEPA-Mandatsreferenz */
    public static final String SEPA_MANDATE_REFERENCE   = "SEPA-Mandatsreferenz";

    /** Pos. 106 — Skontosperre (1 = gesperrt) */
    public static final String CASH_DISCOUNT_LOCK       = "Skontosperre";

    /** Pos. 107 — Gesellschaftername */
    public static final String PARTNER_NAME             = "Gesellschaftername";

    /** Pos. 108 — Beteiligtennummer */
    public static final String PARTICIPANT_NUMBER       = "Beteiligtennummer";

    /** Pos. 109 — Identifikationsnummer */
    public static final String IDENTIFICATION_NUMBER    = "Identifikationsnummer";

    /** Pos. 110 — Zeichnernummer */
    public static final String SIGNATORY_NUMBER         = "Zeichnernummer";

    /** Pos. 111 — Postensperre bis (DDMMYYYY) */
    public static final String ITEM_LOCK_UNTIL          = "Postensperre bis";

    /** Pos. 112 — Bezeichnung SoBil-Sachverhalt */
    public static final String SOBIL_SUBJECT_LABEL      = "Bezeichnung SoBil-Sachverhalt";

    /** Pos. 113 — Kennzeichen SoBil-Buchung */
    public static final String SOBIL_BOOKING_FLAG       = "Kennzeichen SoBil-Buchung";

    /** Pos. 114 — Festschreibung (1 = festgeschrieben) */
    public static final String LOCK_FLAG                = "Festschreibung";

    // -------------------------------------------------------------------------
    // Columns 115–125: Dates, tax, and newer fields
    // -------------------------------------------------------------------------

    /** Pos. 115 — Leistungsdatum (DDMMYYYY) */
    public static final String SERVICE_DATE             = "Leistungsdatum";

    /** Pos. 116 — Datum Zuord. Steuerperiode (DDMMYYYY) */
    public static final String TAX_PERIOD_DATE          = "Datum Zuord. Steuerperiode";

    /** Pos. 117 — Fälligkeit (DDMMYYYY) */
    public static final String DUE_DATE                 = "Fälligkeit";

    /** Pos. 118 — Generalumkehr (GU): 1 = Generalumkehr */
    public static final String GENERAL_REVERSAL         = "Generalumkehr (GU)";

    /** Pos. 119 — Steuersatz */
    public static final String TAX_RATE                 = "Steuersatz";

    /** Pos. 120 — Land */
    public static final String COUNTRY                  = "Land";

    /** Pos. 121 — Abrechnungsreferenz */
    public static final String BILLING_REFERENCE        = "Abrechnungsreferenz";

    /** Pos. 122 — BVV-Position (Betriebsvermögensvergleich) */
    public static final String BVV_POSITION             = "BVV-Position (Betriebsvermögensvergleich)";

    /** Pos. 123 — EU-Mitgliedstaat Steuersatz */
    public static final String EU_MEMBER_STATE_TAX_RATE = "EU-Mitgliedstaat Steuersatz";

    /** Pos. 124 — EU-Steuersatz (Leistungsempfänger) */
    public static final String EU_TAX_RATE_RECIPIENT    = "EU-Steuersatz (Leistungsempfänger)";

    /** Pos. 125 — Erlöskonto */
    public static final String REVENUE_ACCOUNT          = "Erlöskonto";

    // -------------------------------------------------------------------------
    // Ordered column list — position i (0-based) = DATEV column i+1 (1-based)
    // -------------------------------------------------------------------------

    /**
     * All 125 column names in their official DATEV order.
     * Index 0 = column 1 (Umsatz), index 124 = column 125 (Erlöskonto).
     * Used by the parser to report the expected column name when a header is missing.
     */
    public static final List<String> ALL_COLUMNS = List.of(
        AMOUNT,                      //   1
        DEBIT_CREDIT_INDICATOR,      //   2
        CURRENCY_CODE,               //   3
        EXCHANGE_RATE,               //   4
        BASE_AMOUNT,                 //   5
        BASE_CURRENCY_CODE,          //   6
        ACCOUNT,                     //   7
        COUNTER_ACCOUNT,             //   8
        BU_KEY,                      //   9
        DOCUMENT_DATE,                //  10
        DOCUMENT_FIELD1,              //  11
        DOCUMENT_FIELD2,              //  12
        CASH_DISCOUNT,               //  13
        BOOKING_TEXT,                //  14
        ITEM_LOCK,                   //  15
        MISC_ADDRESS_NUMBER,         //  16
        PARTNER_BANK,                //  17
        SUBJECT_MATTER,              //  18
        INTEREST_LOCK,               //  19
        DOCUMENT_LINK,                //  20
        DOCUMENT_INFO_TYPE_1,         //  21
        DOCUMENT_INFO_CONTENT_1,      //  22
        DOCUMENT_INFO_TYPE_2,         //  23
        DOCUMENT_INFO_CONTENT_2,      //  24
        DOCUMENT_INFO_TYPE_3,         //  25
        DOCUMENT_INFO_CONTENT_3,      //  26
        DOCUMENT_INFO_TYPE_4,         //  27
        DOCUMENT_INFO_CONTENT_4,      //  28
        DOCUMENT_INFO_TYPE_5,         //  29
        DOCUMENT_INFO_CONTENT_5,      //  30
        DOCUMENT_INFO_TYPE_6,         //  31
        DOCUMENT_INFO_CONTENT_6,      //  32
        DOCUMENT_INFO_TYPE_7,         //  33
        DOCUMENT_INFO_CONTENT_7,      //  34
        DOCUMENT_INFO_TYPE_8,         //  35
        DOCUMENT_INFO_CONTENT_8,      //  36
        COST_CENTRE_1,               //  37
        COST_CENTRE_2,               //  38
        COST_QUANTITY,               //  39
        EU_COUNTRY_AND_VAT_ID,       //  40
        EU_TAX_RATE,                 //  41
        ALT_TAX_METHOD,              //  42
        SUBJECT_MATTER_LL,           //  43
        FUNCTION_SUPPLEMENT_LL,      //  44
        BU49_MAIN_FUNCTION_TYPE,     //  45
        BU49_MAIN_FUNCTION_NO,       //  46
        BU49_FUNCTION_SUPPLEMENT,    //  47
        ADDITIONAL_INFO_TYPE_1,      //  48
        ADDITIONAL_INFO_CONTENT_1,   //  49
        ADDITIONAL_INFO_TYPE_2,      //  50
        ADDITIONAL_INFO_CONTENT_2,   //  51
        ADDITIONAL_INFO_TYPE_3,      //  52
        ADDITIONAL_INFO_CONTENT_3,   //  53
        ADDITIONAL_INFO_TYPE_4,      //  54
        ADDITIONAL_INFO_CONTENT_4,   //  55
        ADDITIONAL_INFO_TYPE_5,      //  56
        ADDITIONAL_INFO_CONTENT_5,   //  57
        ADDITIONAL_INFO_TYPE_6,      //  58
        ADDITIONAL_INFO_CONTENT_6,   //  59
        ADDITIONAL_INFO_TYPE_7,      //  60
        ADDITIONAL_INFO_CONTENT_7,   //  61
        ADDITIONAL_INFO_TYPE_8,      //  62
        ADDITIONAL_INFO_CONTENT_8,   //  63
        ADDITIONAL_INFO_TYPE_9,      //  64
        ADDITIONAL_INFO_CONTENT_9,   //  65
        ADDITIONAL_INFO_TYPE_10,     //  66
        ADDITIONAL_INFO_CONTENT_10,  //  67
        ADDITIONAL_INFO_TYPE_11,     //  68
        ADDITIONAL_INFO_CONTENT_11,  //  69
        ADDITIONAL_INFO_TYPE_12,     //  70
        ADDITIONAL_INFO_CONTENT_12,  //  71
        ADDITIONAL_INFO_TYPE_13,     //  72
        ADDITIONAL_INFO_CONTENT_13,  //  73
        ADDITIONAL_INFO_TYPE_14,     //  74
        ADDITIONAL_INFO_CONTENT_14,  //  75
        ADDITIONAL_INFO_TYPE_15,     //  76
        ADDITIONAL_INFO_CONTENT_15,  //  77
        ADDITIONAL_INFO_TYPE_16,     //  78
        ADDITIONAL_INFO_CONTENT_16,  //  79
        ADDITIONAL_INFO_TYPE_17,     //  80
        ADDITIONAL_INFO_CONTENT_17,  //  81
        ADDITIONAL_INFO_TYPE_18,     //  82
        ADDITIONAL_INFO_CONTENT_18,  //  83
        ADDITIONAL_INFO_TYPE_19,     //  84
        ADDITIONAL_INFO_CONTENT_19,  //  85
        ADDITIONAL_INFO_TYPE_20,     //  86
        ADDITIONAL_INFO_CONTENT_20,  //  87
        QUANTITY,                    //  88
        WEIGHT,                      //  89
        PAYMENT_METHOD,              //  90
        CLAIM_TYPE,                  //  91
        ASSESSMENT_YEAR,             //  92
        ASSIGNED_DUE_DATE,           //  93
        CASH_DISCOUNT_TYPE,          //  94
        ORDER_NUMBER,                //  95
        BOOKING_TYPE,                //  96
        VAT_KEY_ADVANCE,             //  97
        EU_COUNTRY_ADVANCE,          //  98
        SUBJECT_MATTER_LL_ADVANCE,   //  99
        EU_TAX_RATE_ADVANCE,         // 100
        REVENUE_ACCOUNT_ADVANCE,     // 101
        ORIGIN_CODE,                 // 102
        BOOKING_GUID,                // 103
        COST_DATE,                   // 104
        SEPA_MANDATE_REFERENCE,      // 105
        CASH_DISCOUNT_LOCK,          // 106
        PARTNER_NAME,                // 107
        PARTICIPANT_NUMBER,          // 108
        IDENTIFICATION_NUMBER,       // 109
        SIGNATORY_NUMBER,            // 110
        ITEM_LOCK_UNTIL,             // 111
        SOBIL_SUBJECT_LABEL,         // 112
        SOBIL_BOOKING_FLAG,          // 113
        LOCK_FLAG,                   // 114
        SERVICE_DATE,                // 115
        TAX_PERIOD_DATE,             // 116
        DUE_DATE,                    // 117
        GENERAL_REVERSAL,            // 118
        TAX_RATE,                    // 119
        COUNTRY,                     // 120
        BILLING_REFERENCE,           // 121
        BVV_POSITION,                // 122
        EU_MEMBER_STATE_TAX_RATE,    // 123
        EU_TAX_RATE_RECIPIENT,       // 124
        REVENUE_ACCOUNT              // 125
    );

    // -------------------------------------------------------------------------
    // Text columns — values must be enclosed in double quotes in the CSV output.
    // Purely numeric columns (amounts, account numbers, flags, dates as DDMMYYYY)
    // are NOT listed here and are written without quotes.
    // Source: DATEV EXTF Buchungsstapel "Ausdruck" (regex) column in the format spec.
    // -------------------------------------------------------------------------

    /**
     * Columns whose values must be quoted in the EXTF CSV output.
     *
     * <p>A value that is not in this set but contains a semicolon, double quote,
     * or line break will still be quoted by the writer (safety quoting).
     *
     * <p><b>Verify against your official DATEV documentation</b> — in particular for
     * fields that the spec marks as numeric codes (e.g. Sachverhalt, BU 49 fields).
     */
    public static final Set<String> TEXT_COLUMNS = Set.of(
        DEBIT_CREDIT_INDICATOR,          //   2 — "S" or "H"
        CURRENCY_CODE,                   //   3 — e.g. "EUR"
        BASE_CURRENCY_CODE,              //   6 — e.g. "EUR"
        DOCUMENT_FIELD1,                  //  11 — invoice number
        DOCUMENT_FIELD2,                  //  12 — secondary reference
        BOOKING_TEXT,                    //  14 — free text
        MISC_ADDRESS_NUMBER,             //  16 — alphanumeric
        PARTNER_BANK,                    //  17 — alphanumeric
        ALT_TAX_METHOD,                  //  42 — e.g. "I", "S", "K"
        SUBJECT_MATTER_LL,               //  43 — alphanumeric code
        FUNCTION_SUPPLEMENT_LL,          //  44 — alphanumeric code
        DOCUMENT_LINK,                    //  20 — BEDI + UUID
        DOCUMENT_INFO_TYPE_1,             //  21
        DOCUMENT_INFO_CONTENT_1,          //  22
        DOCUMENT_INFO_TYPE_2,             //  23
        DOCUMENT_INFO_CONTENT_2,          //  24
        DOCUMENT_INFO_TYPE_3,             //  25
        DOCUMENT_INFO_CONTENT_3,          //  26
        DOCUMENT_INFO_TYPE_4,             //  27
        DOCUMENT_INFO_CONTENT_4,          //  28
        DOCUMENT_INFO_TYPE_5,             //  29
        DOCUMENT_INFO_CONTENT_5,          //  30
        DOCUMENT_INFO_TYPE_6,             //  31
        DOCUMENT_INFO_CONTENT_6,          //  32
        DOCUMENT_INFO_TYPE_7,             //  33
        DOCUMENT_INFO_CONTENT_7,          //  34
        DOCUMENT_INFO_TYPE_8,             //  35
        DOCUMENT_INFO_CONTENT_8,          //  36
        COST_CENTRE_1,                   //  37 — alphanumeric
        COST_CENTRE_2,                   //  38 — alphanumeric
        EU_COUNTRY_AND_VAT_ID,           //  40 — "DE" + VAT number
        ADDITIONAL_INFO_TYPE_1,          //  48
        ADDITIONAL_INFO_CONTENT_1,       //  49
        ADDITIONAL_INFO_TYPE_2,          //  50
        ADDITIONAL_INFO_CONTENT_2,       //  51
        ADDITIONAL_INFO_TYPE_3,          //  52
        ADDITIONAL_INFO_CONTENT_3,       //  53
        ADDITIONAL_INFO_TYPE_4,          //  54
        ADDITIONAL_INFO_CONTENT_4,       //  55
        ADDITIONAL_INFO_TYPE_5,          //  56
        ADDITIONAL_INFO_CONTENT_5,       //  57
        ADDITIONAL_INFO_TYPE_6,          //  58
        ADDITIONAL_INFO_CONTENT_6,       //  59
        ADDITIONAL_INFO_TYPE_7,          //  60
        ADDITIONAL_INFO_CONTENT_7,       //  61
        ADDITIONAL_INFO_TYPE_8,          //  62
        ADDITIONAL_INFO_CONTENT_8,       //  63
        ADDITIONAL_INFO_TYPE_9,          //  64
        ADDITIONAL_INFO_CONTENT_9,       //  65
        ADDITIONAL_INFO_TYPE_10,         //  66
        ADDITIONAL_INFO_CONTENT_10,      //  67
        ADDITIONAL_INFO_TYPE_11,         //  68
        ADDITIONAL_INFO_CONTENT_11,      //  69
        ADDITIONAL_INFO_TYPE_12,         //  70
        ADDITIONAL_INFO_CONTENT_12,      //  71
        ADDITIONAL_INFO_TYPE_13,         //  72
        ADDITIONAL_INFO_CONTENT_13,      //  73
        ADDITIONAL_INFO_TYPE_14,         //  74
        ADDITIONAL_INFO_CONTENT_14,      //  75
        ADDITIONAL_INFO_TYPE_15,         //  76
        ADDITIONAL_INFO_CONTENT_15,      //  77
        ADDITIONAL_INFO_TYPE_16,         //  78
        ADDITIONAL_INFO_CONTENT_16,      //  79
        ADDITIONAL_INFO_TYPE_17,         //  80
        ADDITIONAL_INFO_CONTENT_17,      //  81
        ADDITIONAL_INFO_TYPE_18,         //  82
        ADDITIONAL_INFO_CONTENT_18,      //  83
        ADDITIONAL_INFO_TYPE_19,         //  84
        ADDITIONAL_INFO_CONTENT_19,      //  85
        ADDITIONAL_INFO_TYPE_20,         //  86
        ADDITIONAL_INFO_CONTENT_20,      //  87
        ORDER_NUMBER,                    //  95 — alphanumeric
        EU_COUNTRY_ADVANCE,              //  98 — 2-letter country code
        SUBJECT_MATTER_LL_ADVANCE,       //  99 — alphanumeric code
        ORIGIN_CODE,                     // 102 — 2-char code
        BOOKING_GUID,                    // 103 — UUID
        SEPA_MANDATE_REFERENCE,          // 105 — alphanumeric
        PARTNER_NAME,                    // 107 — free text
        PARTICIPANT_NUMBER,              // 108 — alphanumeric
        IDENTIFICATION_NUMBER,           // 109 — alphanumeric
        SIGNATORY_NUMBER,                // 110 — alphanumeric
        SOBIL_SUBJECT_LABEL,             // 112 — free text
        COUNTRY,                         // 120 — 2-letter code
        BILLING_REFERENCE,               // 121 — alphanumeric
        BVV_POSITION                     // 122 — alphanumeric
    );
}
