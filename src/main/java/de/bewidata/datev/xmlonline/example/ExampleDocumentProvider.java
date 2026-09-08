package de.bewidata.datev.xmlonline.example;

import de.bewidata.datev.xmlonline.model.BookingLine;
import de.bewidata.datev.xmlonline.model.SourceDocument;
import de.bewidata.datev.xmlonline.plugin.DocumentProviderResult;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Example implementation of {@link SourceDocumentProvider} for demo and testing purposes.
 *
 * <p>On first use, 10 minimal sample invoice PDFs are generated into a temporary directory.
 * For each booking line, one PDF is selected at random. The document type (incoming/outgoing)
 * is derived from the first character of the invoice number field (Belegfeld 1):
 * prefix {@code "RA"} → outgoing, everything else → incoming.
 *
 * <p><b>Not intended for production use.</b> Replace with an application-specific
 * implementation that queries your DMS (e.g. MÖBELPILOT).
 *
 * <p>German: <em>Beispiel-Belegrecherche</em>
 */
public class ExampleDocumentProvider implements SourceDocumentProvider {

    private static final int NUM_SAMPLES = 10;

    private final List<Path> samplePdfs;
    private final Random random;

    /**
     * Creates the provider, generating 10 sample PDFs in a temp directory.
     *
     * @throws IOException if the temp directory or PDF files cannot be created
     */
    public ExampleDocumentProvider() throws IOException {
        this.random     = new Random();
        this.samplePdfs = generateSamplePdfs();
    }

    @Override
    public DocumentProviderResult findDocuments(BookingLine line) {
        Path selectedPdf = samplePdfs.get(random.nextInt(samplePdfs.size()));

        // Derive document type from Belegfeld 1: "RA-..." → outgoing, else → incoming
        String raw = line.getDocumentField1();
        String documentField = raw != null ? raw.trim().toUpperCase() : "";
        boolean isOutgoing = documentField.startsWith("RA");

        String description = "Musterbeleg " + (raw != null ? raw : "");

        SourceDocument doc = isOutgoing
                ? SourceDocument.outgoing(selectedPdf).description(description).build()
                : SourceDocument.incoming(selectedPdf).description(description).build();

        // Roughly 50 % of the time supply an external GUID so developers can see the feature.
        String guid = random.nextBoolean() ? UUID.randomUUID().toString().toUpperCase() : null;
        return guid != null
                ? DocumentProviderResult.of(List.of(doc), guid)
                : DocumentProviderResult.of(List.of(doc));
    }

    private static List<Path> generateSamplePdfs() throws IOException {
        Path tempDir = Files.createTempDirectory("datev-helper-sample-pdfs-");
        tempDir.toFile().deleteOnExit();

        List<Path> pdfs = new ArrayList<>(NUM_SAMPLES);
        for (int i = 0; i < NUM_SAMPLES; i++) {
            Path pdf = tempDir.resolve(String.format("sample-invoice-%02d.pdf", i + 1));
            Files.write(pdf, SamplePdfGenerator.generate(i));
            pdf.toFile().deleteOnExit();
            pdfs.add(pdf);
        }
        return pdfs;
    }
}
