package de.bewidata.datev.xmlonline.zip;

import de.bewidata.datev.xmlonline.DATEVXmlConfig;
import de.bewidata.datev.xmlonline.extf.ExtfWriter;
import de.bewidata.datev.xmlonline.model.BookingBatch;
import de.bewidata.datev.xmlonline.model.BookingLineWithDocuments;
import de.bewidata.datev.xmlonline.model.SourceDocument;
import de.bewidata.datev.xmlonline.xml.DocumentXmlBuilder;

import jakarta.xml.bind.JAXBException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds DATEV-compliant ZIP archives from a group of booking lines.
 *
 * <p>DATEV size limits (configurable in {@link DATEVXmlConfig}, defaults per DATEV recommendation):
 * <ul>
 *   <li>Max 100 MB per ZIP archive</li>
 *   <li>Max 20 MB per individual document</li>
 * </ul>
 * If a group exceeds the ZIP size limit, it is split sequentially into sub-groups.
 *
 * <p>German: <em>ZipArchivBuilder</em>
 */
public class ZipArchiveBuilder {

    private static final String DOCUMENT_XML = "document.xml";

    private final DATEVXmlConfig config;
    private final ExtfWriter extfWriter = new ExtfWriter();
    private Consumer<String> hintListener = message -> {};

    public ZipArchiveBuilder(DATEVXmlConfig config) {
        this.config = config;
    }

    /**
     * Registers a listener notified with an informational message whenever the same
     * source document (identical local path) is referenced by more than one booking
     * line. This is expected in practice (one document can belong to several bookings)
     * and is not an error; the document is still written to the archive only once.
     *
     * @param listener callback; {@code null} disables any previously registered listener
     * @return this instance for fluent chaining
     */
    public ZipArchiveBuilder withHintListener(Consumer<String> listener) {
        this.hintListener = listener != null ? listener : message -> {};
        return this;
    }

    /**
     * Builds one or more ZIP archives for the given group.
     *
     * @param batch      the full booking batch (provides EXTF metadata and column names)
     * @param group      booking lines in this group (after split strategy has been applied)
     * @param outputDir  target directory for the ZIP files
     * @param namePrefix prefix for output file names (e.g. {@code "batch_incoming"})
     * @return list of created ZIP file paths
     */
    public List<Path> buildZips(BookingBatch batch,
                                List<BookingLineWithDocuments> group,
                                Path outputDir,
                                String namePrefix) throws IOException, JAXBException {

        List<List<BookingLineWithDocuments>> subGroups = splitBySize(group);
        List<Path> results = new ArrayList<>();

        for (int i = 0; i < subGroups.size(); i++) {
            String fileName = subGroups.size() == 1
                    ? namePrefix + ".zip"
                    : namePrefix + "_" + (i + 1) + ".zip";
            results.add(writeZip(batch, subGroups.get(i), outputDir.resolve(fileName)));
        }
        return results;
    }

    private Path writeZip(BookingBatch batch,
                          List<BookingLineWithDocuments> lines,
                          Path targetPath) throws IOException, JAXBException {

        DocumentXmlBuilder xmlBuilder = new DocumentXmlBuilder(config);

        // Build document.xml — also sets Beleglink in each BookingLine as a side effect
        ByteArrayOutputStream xmlBytes = new ByteArrayOutputStream();
        try {
            xmlBuilder.build(lines, xmlBytes);
        } catch (JAXBException e) {
            throw new JAXBException(
                "Failed to build document.xml for '" + targetPath.getFileName() + "': " + e.getMessage(), e);
        }

        // Build CSV content (Beleglink values are now set) and write alongside the ZIP
        StringWriter csvWriter = new StringWriter();
        extfWriter.write(batch, lines.stream().map(BookingLineWithDocuments::line).toList(), csvWriter);
        String baseName = targetPath.getFileName().toString();
        String csvName  = baseName.endsWith(".zip")
                ? baseName.substring(0, baseName.length() - 4) + ".csv"
                : baseName + ".csv";
        Files.writeString(targetPath.resolveSibling(csvName),
                csvWriter.toString(), StandardCharsets.UTF_8);

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(targetPath))) {

            zip.putNextEntry(new ZipEntry(DOCUMENT_XML));
            zip.write(xmlBytes.toByteArray());
            zip.closeEntry();

            Map<String, Path> writtenEntries = new HashMap<>();
            for (BookingLineWithDocuments lwd : lines) {
                for (SourceDocument doc : lwd.documents()) {
                    Path previous = writtenEntries.get(doc.getFileName());
                    if (previous != null) {
                        if (previous.equals(doc.getLocalPath())) {
                            hintListener.accept(String.format(
                                "Document '%s' is referenced by more than one booking line "
                                        + "and has already been added to the archive.",
                                doc.getFileName()));
                            continue;
                        }
                        throw new IOException(String.format(
                            "File name conflict in archive '%s': '%s' refers to both '%s' and '%s'.",
                            targetPath.getFileName(), doc.getFileName(), previous, doc.getLocalPath()));
                    }
                    writtenEntries.put(doc.getFileName(), doc.getLocalPath());

                    zip.putNextEntry(new ZipEntry(doc.getFileName()));
                    try {
                        Files.copy(doc.getLocalPath(), zip);
                    } catch (IOException e) {
                        throw new IOException(String.format(
                            "Failed to add document '%s' to archive '%s': %s",
                            doc.getLocalPath(), targetPath.getFileName(), e.getMessage()), e);
                    }
                    zip.closeEntry();
                }
            }
        }
        return targetPath;
    }

    /**
     * Splits a group into sub-groups so each sub-group fits within the max ZIP size.
     */
    private List<List<BookingLineWithDocuments>> splitBySize(List<BookingLineWithDocuments> group) {
        long maxBytes = config.getMaxZipSize();
        List<List<BookingLineWithDocuments>> subGroups = new ArrayList<>();
        List<BookingLineWithDocuments> current = new ArrayList<>();
        long currentSize = 0;

        for (BookingLineWithDocuments lwd : group) {
            long lineSize = lwd.totalSize();
            if (!current.isEmpty() && currentSize + lineSize > maxBytes) {
                subGroups.add(current);
                current = new ArrayList<>();
                currentSize = 0;
            }
            current.add(lwd);
            currentSize += lineSize;
        }
        if (!current.isEmpty()) subGroups.add(current);
        return subGroups.isEmpty() ? List.of(List.of()) : subGroups;
    }
}
