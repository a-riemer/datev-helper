package de.bewidata.datev.xmlonline;

import de.bewidata.datev.xmlonline.extf.ExtfParser;
import de.bewidata.datev.xmlonline.model.BookingBatch;
import de.bewidata.datev.xmlonline.model.BookingLine;
import de.bewidata.datev.xmlonline.model.BookingLineWithDocuments;
import de.bewidata.datev.xmlonline.model.SourceDocument;
import de.bewidata.datev.xmlonline.plugin.DocumentProviderResult;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentException;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentProvider;
import de.bewidata.datev.xmlonline.split.BatchSplitStrategy;
import de.bewidata.datev.xmlonline.split.ByDocumentTypeSplitStrategy;
import de.bewidata.datev.xmlonline.zip.ZipArchiveBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Main API of the DATEV XML interface online (XML-Schnittstelle online).
 *
 * <p>Reads a DATEV booking batch (EXTF CSV), resolves source documents via
 * a {@link SourceDocumentProvider} plugin, and produces DATEV-compliant ZIP archives.
 *
 * <p>German: <em>DATEVXmlSchnittstelle</em>
 *
 * <pre>
 * DATEVXmlConfig config = DATEVXmlConfig.builder()
 *     .consultantNumber(12345)
 *     .clientNumber(1)
 *     .clientName("Musterfirma GmbH")
 *     .build();
 *
 * List&lt;Path&gt; zips = new DATEVXmlProcessor(config, new MyDocumentProvider())
 *     .process(Path.of("buchungsstapel.csv"), Path.of("output/"));
 * </pre>
 */
public class DATEVXmlProcessor {

    private final DATEVXmlConfig config;
    private final SourceDocumentProvider provider;
    private final BatchSplitStrategy splitStrategy;
    private final ExtfParser parser;
    private Consumer<BookingLineWithDocuments> lineListener;
    private Consumer<String> hintListener = message -> {};

    public DATEVXmlProcessor(DATEVXmlConfig config, SourceDocumentProvider provider) {
        this(config, provider, new ByDocumentTypeSplitStrategy());
    }

    public DATEVXmlProcessor(DATEVXmlConfig config,
                             SourceDocumentProvider provider,
                             BatchSplitStrategy splitStrategy) {
        this.config        = config;
        this.provider      = provider;
        this.splitStrategy = splitStrategy;
        this.parser        = new ExtfParser();
    }

    /**
     * Registers a listener that is called once for each booking line after its documents
     * have been resolved. The listener receives the fully populated {@link BookingLineWithDocuments}
     * including the {@link de.bewidata.datev.xmlonline.plugin.FindStatus} from the provider.
     *
     * <p>Intended for verbose/diagnostic output. The listener must not throw checked exceptions.
     *
     * @param listener callback; {@code null} disables any previously registered listener
     * @return this instance for fluent chaining
     */
    public DATEVXmlProcessor withLineListener(Consumer<BookingLineWithDocuments> listener) {
        this.lineListener = listener;
        return this;
    }

    /**
     * Registers a listener notified with an informational message whenever the same
     * source document is referenced by more than one booking line (allowed and expected;
     * the document is still written to each ZIP archive only once).
     *
     * @param listener callback; {@code null} disables any previously registered listener
     * @return this instance for fluent chaining
     */
    public DATEVXmlProcessor withHintListener(Consumer<String> listener) {
        this.hintListener = listener != null ? listener : message -> {};
        return this;
    }

    /**
     * Reads the booking batch, resolves source documents, and produces ZIP archives.
     *
     * @param batchCsv   input EXTF CSV file (read-only, not modified)
     * @param outputDir  directory for the generated ZIP files
     * @return           paths of the generated ZIP archives
     * @throws IOException              on file I/O errors
     * @throws DocumentTooLargeException if a document exceeds the configured size limit
     * @throws SourceDocumentException  if the document provider fails
     */
    public List<Path> process(Path batchCsv, Path outputDir)
            throws IOException, DocumentTooLargeException, SourceDocumentException {

        Files.createDirectories(outputDir);

        BookingBatch batch;
        try {
            batch = parser.parse(batchCsv);
        } catch (IOException e) {
            throw new IOException("Failed to read booking batch '" + batchCsv.toAbsolutePath() + "': " + e.getMessage(), e);
        }

        // Merge: explicit config takes priority; values from the EXTF header are the fallback.
        DATEVXmlConfig effectiveConfig = mergeWithBatch(config, batch);
        ZipArchiveBuilder zipBuilder = new ZipArchiveBuilder(effectiveConfig)
                .withHintListener(hintListener);

        List<BookingLineWithDocuments> allLines = resolveDocuments(batch);
        validateSizes(allLines);

        List<List<BookingLineWithDocuments>> groups = splitStrategy.split(allLines);

        String csvBaseName = baseName(batchCsv);
        List<Path> results = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            String prefix = groups.size() == 1
                    ? csvBaseName
                    : csvBaseName + "_group" + (i + 1);
            try {
                results.addAll(zipBuilder.buildZips(batch, groups.get(i), outputDir, prefix));
            } catch (jakarta.xml.bind.JAXBException e) {
                throw new IOException("Failed to build document.xml", e);
            }
        }
        return results;
    }

    private List<BookingLineWithDocuments> resolveDocuments(BookingBatch batch)
            throws SourceDocumentException {
        List<BookingLineWithDocuments> result = new ArrayList<>();
        for (BookingLine line : batch.getLines()) {
            DocumentProviderResult providerResult;
            try {
                providerResult = provider.findDocuments(line);
            } catch (SourceDocumentException e) {
                throw new SourceDocumentException(String.format(
                    "Document lookup failed for booking line %d (Belegfeld 1: '%s'): %s",
                    line.getLineNumber(), line.getDocumentField1(), e.getMessage()), e);
            }
            BookingLineWithDocuments lwd = new BookingLineWithDocuments(
                    line,
                    providerResult.getDocuments(),
                    providerResult.getGuid().orElse(null),
                    providerResult.getStatus(),
                    providerResult.getMessage().orElse(null));
            if (lineListener != null) {
                lineListener.accept(lwd);
            }
            result.add(lwd);
        }
        return result;
    }

    /**
     * Returns a config that fills any unset fields (null / blank) from the EXTF batch header.
     * Explicitly set values in {@code config} always take priority.
     */
    private static DATEVXmlConfig mergeWithBatch(DATEVXmlConfig config, BookingBatch batch) {
        Long   consultantNumber = config.getConsultantNumber() != null
                ? config.getConsultantNumber() : batch.getConsultantNumber();
        Long   clientNumber     = config.getClientNumber() != null
                ? config.getClientNumber() : batch.getClientNumber();
        String description      = isSet(config.getDescription())
                ? config.getDescription() : batch.getDescription();

        // Short-circuit: nothing to merge.
        if (Objects.equals(consultantNumber, config.getConsultantNumber())
                && Objects.equals(clientNumber, config.getClientNumber())
                && Objects.equals(description, config.getDescription())) {
            return config;
        }

        DATEVXmlConfig.Builder b = DATEVXmlConfig.builder();
        if (consultantNumber != null) b.consultantNumber(consultantNumber);
        if (clientNumber != null)     b.clientNumber(clientNumber);
        if (isSet(config.getClientName())) b.clientName(config.getClientName());
        if (isSet(description))       b.description(description);
        b.maxZipSize(config.getMaxZipSize());
        b.maxDocumentSize(config.getMaxDocumentSize());
        return b.build();
    }

    private static boolean isSet(String s) {
        return s != null && !s.isBlank();
    }

    private static String baseName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private void validateSizes(List<BookingLineWithDocuments> lines)
            throws DocumentTooLargeException {
        long limit = config.getMaxDocumentSize();
        for (BookingLineWithDocuments lwd : lines) {
            for (SourceDocument doc : lwd.documents()) {
                long size = doc.fileSize();
                if (size > limit) {
                    throw new DocumentTooLargeException(doc.getLocalPath(), size, limit);
                }
            }
        }
    }
}
