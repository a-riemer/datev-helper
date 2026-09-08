package de.bewidata.datev.xmlonline.provider;

import de.bewidata.datev.xmlonline.model.BookingLine;
import de.bewidata.datev.xmlonline.model.SourceDocument;
import de.bewidata.datev.xmlonline.plugin.DocumentProviderResult;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentException;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A generic {@link SourceDocumentProvider} that locates booking documents by GUID.
 *
 * <h2>Convention</h2>
 * <p>The caller pre-places document files in a configured directory. Each file must be
 * named {@code <GUID>.<ext>} (any extension), where {@code <GUID>} is the same UUID
 * that appears in the booking line's {@code Beleglink} column (format: {@code BEDI"<GUID>"}).
 *
 * <p>The provider extracts the GUID from {@code Beleglink}, looks for the matching
 * file, and returns the GUID as an external GUID so the library preserves it
 * unchanged instead of generating a new one.
 *
 * <h2>Prerequisites</h2>
 * <ul>
 *   <li>The input EXTF CSV must have the {@code Beleglink} column pre-populated.</li>
 *   <li>A file named {@code <GUID>.<ext>} (any extension) must exist in the configured
 *       directory for each booking line that should have a document attached. If
 *       multiple files share the same GUID base name, the first one found is used.</li>
 *   <li>Booking lines whose {@code Beleglink} is empty or whose PDF file is absent
 *       are silently skipped (no document attached).</li>
 * </ul>
 *
 * <h2>Configuration parameters</h2>
 * <table>
 *   <tr><th>Key</th><th>Required</th><th>Description</th></tr>
 *   <tr><td>{@code directory}</td><td>yes</td><td>Path to the directory containing the document files</td></tr>
 *   <tr><td>{@code type}</td><td>no</td><td>{@code eingang} (default) or {@code ausgang}</td></tr>
 * </table>
 *
 * <h2>CLI usage</h2>
 * <pre>
 * java -jar datev-helper-cli.jar \
 *     --provider-class de.bewidata.datev.xmlonline.provider.FileByGUIDProvider \
 *     --provider-param directory=/var/data/belege \
 *     --provider-param type=eingang \
 *     buchungsstapel.csv output/
 * </pre>
 */
public class FileByGUIDProvider implements SourceDocumentProvider {

    private static final String PARAM_DIRECTORY = "directory";
    private static final String PARAM_TYPE      = "type";

    private Path directory;
    private boolean outgoing = false;

    @Override
    public void configure(Map<String, String> params) throws Exception {
        String dir = params.get(PARAM_DIRECTORY);
        if (dir == null || dir.isBlank()) {
            throw new IllegalArgumentException(
                "FileByGUIDProvider requires the parameter 'directory' "
                + "(path to the folder containing <GUID>.<ext> files).");
        }
        directory = Path.of(dir);
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException(
                "FileByGUIDProvider: Verzeichnis existiert nicht: " + dir);
        }

        boolean hasFiles;
        try (var stream = Files.list(directory)) {
            hasFiles = stream.anyMatch(Files::isRegularFile);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "FileByGUIDProvider: Directory not readable: " + dir + " — " + e.getMessage());
        }
        if (!hasFiles) {
            throw new IllegalArgumentException(
                "FileByGUIDProvider: Directory has no files: " + dir);
        }

        String type = params.getOrDefault(PARAM_TYPE, "eingang").strip().toLowerCase();
        outgoing = "ausgang".equals(type);
    }

    @Override
    public Optional<String> configSummary() {
        return directory != null
                ? Optional.of("Search directory: " + directory.toAbsolutePath())
                : Optional.empty();
    }

    @Override
    public DocumentProviderResult findDocuments(BookingLine line) throws SourceDocumentException {
        if (directory == null) {
            throw new SourceDocumentException(
                "FileByGUIDProvider has not been configured — call configure() before use.");
        }

        String documentLink = line.getDocumentLink();
        if (documentLink == null || documentLink.isBlank()) {
            return DocumentProviderResult.skipped("No document link (Beleglink)");
        }

        String guid = extractGuid(documentLink);
        if (guid == null) {
            return DocumentProviderResult.skipped(
                    "Document link not in exptected format BEDI \"<UUID>\": " + documentLink);
        }

        Path docPath = findByGuid(guid);
        if (docPath == null) {
            return DocumentProviderResult.notFound("No file for GUID: " + guid);
        }

        SourceDocument doc = outgoing
                ? SourceDocument.outgoing(docPath).build()
                : SourceDocument.incoming(docPath).build();

        // Return the same GUID so the library keeps the pre-assigned Beleglink stable.
        return DocumentProviderResult.of(List.of(doc), guid)
                .withMessage("found: " + docPath.getFileName());
    }

    /**
     * Scans {@link #directory} for a regular file whose base name (without extension)
     * equals {@code guid}. Returns the first match, or {@code null} if none is found.
     */
    private Path findByGuid(String guid) throws SourceDocumentException {
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> baseName(p).equals(guid))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new SourceDocumentException(
                    "Failed to scan document directory '" + directory + "': " + e.getMessage(), e);
        }
    }

    private static String baseName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * Extracts the raw UUID from a Beleglink value.
     *
     * <p>The Beleglink field value has the format {@code BEDI"<UUID>"} (with literal
     * double-quote characters around the UUID). Returns {@code null} if the value
     * does not match this format.
     */
    static String extractGuid(String documentLink) {
        if (documentLink == null) return null;
        // Expected format: BEDI "<UUID>"
        if (!documentLink.startsWith("BEDI \"")) return null;
        String inner = documentLink.substring(6); // strip BEDI "
        if (!inner.endsWith("\"") || inner.length() < 2) return null;
        return inner.substring(0, inner.length() - 1); // strip trailing "
    }
}
