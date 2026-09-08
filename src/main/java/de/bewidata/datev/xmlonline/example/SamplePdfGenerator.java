package de.bewidata.datev.xmlonline.example;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Generates minimal but valid PDF documents for demo and testing purposes.
 *
 * <p>Each generated PDF represents a fictional German invoice with invoice number,
 * company name, date, and amount. The PDF structure follows ISO 32000-1 (PDF 1.4).
 *
 * <p>Not intended for production use — demo only.
 */
public class SamplePdfGenerator {

    /** Ten fictional invoice datasets: {invoiceNo, company, date, amount, type} */
    private static final String[][] INVOICES = {
        {"RE-2024-0001", "Muster GmbH",               "15.01.2024", "1.234,56 EUR", "Eingangsrechnung"},
        {"RE-2024-0002", "Beispiel Handels AG",        "18.01.2024",   "567,89 EUR", "Eingangsrechnung"},
        {"RA-2024-0001", "Testfirma und Partner KG",   "20.01.2024", "3.450,00 EUR", "Ausgangsrechnung"},
        {"RE-2024-0003", "Lieferant Nord GmbH",        "22.01.2024",   "899,00 EUR", "Eingangsrechnung"},
        {"RA-2024-0002", "Kunde Sued GmbH & Co. KG",   "24.01.2024", "2.100,00 EUR", "Ausgangsrechnung"},
        {"RE-2024-0004", "IT-Service West UG",         "25.01.2024",   "349,95 EUR", "Eingangsrechnung"},
        {"RA-2024-0003", "Grossabnehmer Ost GmbH",     "26.01.2024", "7.800,00 EUR", "Ausgangsrechnung"},
        {"RE-2024-0005", "Transport Logistik AG",      "28.01.2024",   "640,20 EUR", "Eingangsrechnung"},
        {"RA-2024-0004", "Stammkunde Berlin GmbH",     "29.01.2024", "1.050,00 EUR", "Ausgangsrechnung"},
        {"RE-2024-0006", "Software Solutions GmbH",   "31.01.2024", "4.999,00 EUR", "Eingangsrechnung"},
    };

    /**
     * Generates a sample invoice PDF for the given index (0–9, wraps around).
     *
     * @param index invoice index (modulo 10)
     * @return raw PDF bytes
     */
    public static byte[] generate(int index) {
        String[] inv = INVOICES[index % INVOICES.length];
        return buildPdf(inv[0], inv[1], inv[2], inv[3], inv[4]);
    }

    private static byte[] buildPdf(String invoiceNo, String company,
                                   String date, String amount, String type) {
        // Page content stream (PDF operators)
        String stream = String.join("\n",
            "BT",
            "/F1 16 Tf",
            "50 800 Td",
            pdfString(type),
            "Tj",
            "0 -30 Td",
            "/F1 11 Tf",
            pdfString("Rechnungsnummer: " + invoiceNo),
            "Tj",
            "0 -18 Td",
            pdfString("Aussteller:      " + company),
            "Tj",
            "0 -18 Td",
            pdfString("Datum:           " + date),
            "Tj",
            "0 -18 Td",
            pdfString("Betrag:          " + amount),
            "Tj",
            "0 -40 Td",
            "/F1 8 Tf",
            pdfString("Dieses Dokument ist ein Musterbeleg fuer Demo-Zwecke."),
            "Tj",
            "ET"
        );
        byte[] streamBytes = stream.getBytes(StandardCharsets.ISO_8859_1);

        // Build PDF using tracked byte positions
        ByteArrayOutputStream buf = new ByteArrayOutputStream(2048);
        int[] offsets = new int[6]; // objects 1–5

        write(buf, "%PDF-1.4\n");
        write(buf, "%äüöß\n"); // binary comment (4 bytes > 127)

        offsets[1] = buf.size();
        write(buf, "1 0 obj\n<</Type /Catalog /Pages 2 0 R>>\nendobj\n");

        offsets[2] = buf.size();
        write(buf, "2 0 obj\n<</Type /Pages /Kids [3 0 R] /Count 1>>\nendobj\n");

        offsets[3] = buf.size();
        write(buf, "3 0 obj\n<</Type /Page /Parent 2 0 R /MediaBox [0 0 595 842]"
                + " /Contents 4 0 R /Resources <</Font <</F1 5 0 R>>>>>>\nendobj\n");

        offsets[4] = buf.size();
        write(buf, "4 0 obj\n<</Length " + streamBytes.length + ">>\nstream\n");
        try { buf.write(streamBytes); } catch (IOException ignored) {}
        write(buf, "\nendstream\nendobj\n");

        offsets[5] = buf.size();
        write(buf, "5 0 obj\n<</Type /Font /Subtype /Type1 /BaseFont /Helvetica>>\nendobj\n");

        int xrefOffset = buf.size();
        write(buf, "xref\n");
        write(buf, "0 6\n");
        write(buf, "0000000000 65535 f \n");
        for (int i = 1; i <= 5; i++) {
            write(buf, String.format("%010d 00000 n \n", offsets[i]));
        }
        write(buf, "trailer\n<</Size 6 /Root 1 0 R>>\n");
        write(buf, "startxref\n");
        write(buf, xrefOffset + "\n");
        write(buf, "%%EOF\n");

        return buf.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String text) {
        try {
            out.write(text.getBytes(StandardCharsets.ISO_8859_1));
        } catch (IOException ignored) {
        }
    }

    /** Wraps a string as a PDF literal string, escaping parentheses. */
    private static String pdfString(String text) {
        return "(" + text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)") + ")";
    }
}
