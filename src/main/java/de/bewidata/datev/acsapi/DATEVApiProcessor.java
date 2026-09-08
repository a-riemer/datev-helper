package de.bewidata.datev.acsapi;

import de.bewidata.datev.acsapi.model.DocumentUploadMetadata;
import de.bewidata.datev.acsapi.model.UploadGroupResult;
import de.bewidata.datev.xmlonline.DocumentTooLargeException;
import de.bewidata.datev.xmlonline.extf.ExtfParser;
import de.bewidata.datev.xmlonline.extf.ExtfWriter;
import de.bewidata.datev.xmlonline.model.BookingBatch;
import de.bewidata.datev.xmlonline.model.BookingLine;
import de.bewidata.datev.xmlonline.model.BookingLineWithDocuments;
import de.bewidata.datev.xmlonline.model.SourceDocument;
import de.bewidata.datev.xmlonline.plugin.DocumentProviderResult;
import de.bewidata.datev.xmlonline.plugin.FindStatus;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentException;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentProvider;
import de.bewidata.datev.xmlonline.split.BatchSplitStrategy;
import de.bewidata.datev.xmlonline.split.ByMonthSplitStrategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Library entry point for uploading a DATEV EXTF booking batch via the Buchungsdatenservice REST API.
 *
 * <p>This class is the API-based counterpart of {@link de.bewidata.datev.xmlonline.DATEVXmlProcessor},
 * which writes local ZIP archives. Both accept any {@link SourceDocumentProvider} implementation.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Parse the EXTF CSV</li>
 *   <li>For each booking line, call the provider to resolve documents</li>
 *   <li>Validate that no individual document exceeds the size limit</li>
 *   <li>Apply the {@link BatchSplitStrategy} to group lines (default: {@link ByMonthSplitStrategy})</li>
 *   <li>For each group:
 *     <ol>
 *       <li>Derive the {@code register} (accounting period) from the group's {@code Belegdatum}
 *           values, unless an explicit {@code register} is set in {@link DocumentUploadMetadata}</li>
 *       <li>Upload each document via {@code PUT /documents/{guid}}</li>
 *       <li>Write the group's lines to a temporary EXTF CSV and import via
 *           {@code POST /extf-files/import}</li>
 *     </ol>
 *   </li>
 *   <li>Return the list of import job IDs — the caller polls via
 *       {@link AccountingExtfFilesClient#awaitCompletion}</li>
 * </ol>
 *
 * <p>If {@code DocumentUploadMetadata.register()} is {@code null}, the accounting period is
 * derived from each group's booking lines. With {@link ByMonthSplitStrategy} (the default),
 * each group automatically corresponds to exactly one month, so the register is unambiguous.
 *
 * <p>Booking lines that have more than one document: the first document is uploaded with the
 * line's GUID. Additional documents are skipped and a warning is logged; use the DATEV
 * {@code /documents/stapled} endpoint (not yet implemented) for multi-document lines.
 *
 * <p>Example:
 * <pre>
 * DATEVApiProcessor processor = new DATEVApiProcessor(
 *     apiConfig, provider, new DocumentUploadMetadata("MyApp", "Belege", null));
 * List&lt;UploadGroupResult&gt; results = processor.upload(Path.of("buchungsstapel.csv"));
 * AccountingExtfFilesClient extfClient = new AccountingExtfFilesClient(apiConfig);
 * for (UploadGroupResult result : results) {
 *     ExtfJobStatus status = extfClient.awaitCompletion(result.jobId(), Duration.ofMinutes(2));
 * }
 * </pre>
 */
public class DATEVApiProcessor {

    /** Default maximum size for a single document (20 MB). */
    public static final long DEFAULT_MAX_DOCUMENT_SIZE = 20L * 1024 * 1024;

    private final DATEVAcsApiConfig          apiConfig;
    private final SourceDocumentProvider     provider;
    private final DocumentUploadMetadata     uploadMeta;
    private       BatchSplitStrategy         splitStrategy   = new ByMonthSplitStrategy();
    private       long                       maxDocumentSize = DEFAULT_MAX_DOCUMENT_SIZE;
    private       Consumer<BookingLineWithDocuments> lineListener;

    public DATEVApiProcessor(DATEVAcsApiConfig      apiConfig,
                              SourceDocumentProvider provider,
                              DocumentUploadMetadata uploadMeta) {
        this.apiConfig  = apiConfig;
        this.provider   = provider;
        this.uploadMeta = uploadMeta;
    }

    /**
     * Registers a listener called once for each booking line after document resolution.
     * Intended for verbose/diagnostic output.
     */
    public DATEVApiProcessor withLineListener(Consumer<BookingLineWithDocuments> listener) {
        this.lineListener = listener;
        return this;
    }

    /** Overrides the maximum allowed size per document (default: 20 MB). */
    public DATEVApiProcessor withMaxDocumentSize(long bytes) {
        this.maxDocumentSize = bytes;
        return this;
    }

    /**
     * Overrides the split strategy (default: {@link ByMonthSplitStrategy}).
     *
     * <p>{@link de.bewidata.datev.xmlonline.split.SequentialSplitStrategy} treats the entire
     * batch as a single group; useful when the caller manages the accounting period manually
     * via an explicit {@code register} in {@link DocumentUploadMetadata}.
     */
    public DATEVApiProcessor withSplitStrategy(BatchSplitStrategy strategy) {
        this.splitStrategy = strategy;
        return this;
    }

    /**
     * Executes the full upload workflow for the given EXTF CSV.
     *
     * <p>Booking lines are grouped by the configured {@link BatchSplitStrategy}
     * (default: {@link ByMonthSplitStrategy}). Each group produces one EXTF import job.
     *
     * @param inputCsv EXTF CSV file (may or may not have pre-assigned GUIDs in Beleglink)
     * @return one {@link UploadGroupResult} per group, each containing the job ID and the
     *         accounting period ({@code register}) used for that group's document uploads
     * @throws IOException               on file I/O errors
     * @throws DocumentTooLargeException if a resolved document exceeds the size limit
     * @throws SourceDocumentException   if the provider fails
     * @throws DATEVAcsApiException      if the DATEV API returns an error
     * @throws InterruptedException      if the thread is interrupted during an HTTP call
     */
    public List<UploadGroupResult> upload(Path inputCsv)
            throws IOException, DocumentTooLargeException, SourceDocumentException,
                   DATEVAcsApiException, InterruptedException {

        BookingBatch batch;
        try {
            batch = new ExtfParser().parse(inputCsv);
        } catch (IOException e) {
            throw new IOException("Failed to parse EXTF CSV '" + inputCsv + "': " + e.getMessage(), e);
        }

        DATEVAcsApiConfig resolvedConfig = resolveConfig(batch);

        List<BookingLineWithDocuments> allLines = resolveDocuments(batch);
        validateSizes(allLines);

        List<List<BookingLineWithDocuments>> groups = splitStrategy.split(allLines);

        AccountingExtfFilesClient extfClient = new AccountingExtfFilesClient(resolvedConfig);
        AccountingDocumentsClient docsClient = new AccountingDocumentsClient(resolvedConfig);

        List<UploadGroupResult> results = new ArrayList<>(groups.size());
        for (List<BookingLineWithDocuments> group : groups) {
            DocumentUploadMetadata groupMeta = resolveGroupMeta(group);
            uploadDocuments(group, docsClient, groupMeta);

            Path tempCsv = writeTempCsv(batch, group);
            try {
                String jobId = extfClient.importExtf(tempCsv);
                results.add(new UploadGroupResult(jobId, groupMeta.register()));
            } finally {
                try { Files.deleteIfExists(tempCsv); } catch (IOException ignored) {}
            }
        }
        return results;
    }

    // ---- config resolution ----

    private DATEVAcsApiConfig resolveConfig(BookingBatch batch) throws IOException {
        if (apiConfig.getDatevClientId() != null) return apiConfig;
        Long consultant = batch.getConsultantNumber();
        Long client     = batch.getClientNumber();
        if (consultant == null || client == null) {
            throw new IOException(
                "--datev-client-id is not specified and cannot be derived from the EXTF CSV " +
                "(Beraternummer or Mandantennummer missing in header row)");
        }
        return apiConfig.withDatevClientId(consultant + "-" + client);
    }

    // ---- register / metadata resolution ----

    /**
     * Returns the upload metadata for a group, with the register derived from the group's
     * booking lines when not explicitly set in {@link #uploadMeta}.
     */
    private DocumentUploadMetadata resolveGroupMeta(List<BookingLineWithDocuments> group) {
        String register = uploadMeta.register();
        if (register == null || register.isBlank()) {
            register = deriveRegister(group);
        }
        return new DocumentUploadMetadata(uploadMeta.category(), uploadMeta.folder(), register);
    }

    private String deriveRegister(List<BookingLineWithDocuments> group) {
        int fallbackYear = LocalDate.now().getYear();
        for (BookingLineWithDocuments lwd : group) {
            String ym = ByMonthSplitStrategy.extractYearMonth(
                    lwd.line().getDocumentDate(), fallbackYear);
            if (ym != null) return ym;
        }
        return "";
    }

    // ---- document resolution ----

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
            if (lineListener != null) lineListener.accept(lwd);
            result.add(lwd);
        }
        return result;
    }

    // ---- size validation ----

    private void validateSizes(List<BookingLineWithDocuments> lines) throws DocumentTooLargeException {
        for (BookingLineWithDocuments lwd : lines) {
            for (SourceDocument doc : lwd.documents()) {
                long size = doc.fileSize();
                if (size > maxDocumentSize) {
                    throw new DocumentTooLargeException(doc.getLocalPath(), size, maxDocumentSize);
                }
            }
        }
    }

    // ---- document upload ----

    private void uploadDocuments(List<BookingLineWithDocuments> lines,
                                  AccountingDocumentsClient docsClient,
                                  DocumentUploadMetadata groupMeta)
            throws DATEVAcsApiException, IOException, InterruptedException {

        for (BookingLineWithDocuments lwd : lines) {
            if (!lwd.hasDocuments()) continue;

            String guid = lwd.getExternalGuid().orElseGet(() -> UUID.randomUUID().toString().toUpperCase());

            List<SourceDocument> docs = lwd.documents();
            if (docs.size() > 1) {
                System.err.printf(
                    "Warning: booking line %d has %d documents — only the first is uploaded " +
                    "(stapled upload not yet supported). GUID: %s%n",
                    lwd.line().getLineNumber(), docs.size(), guid);
            }

            docsClient.uploadWithGuid(guid, docs.get(0).getLocalPath(), groupMeta);

            // Set Beleglink — same format as DocumentXmlBuilder.
            lwd.line().setDocumentLink("BEDI \"" + guid + "\"");
        }
    }

    // ---- CSV writing ----

    private static Path writeTempCsv(BookingBatch batch, List<BookingLineWithDocuments> lines)
            throws IOException {

        Path temp = Files.createTempFile("datev-upload-", ".csv");
        List<BookingLine> bookingLines = lines.stream()
                .map(BookingLineWithDocuments::line)
                .toList();
        new ExtfWriter().write(batch, bookingLines, temp);
        return temp;
    }
}
