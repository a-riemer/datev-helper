package de.bewidata.datev.xmlonline.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads an OAuth Bearer token from stdin or from a temporary file.
 *
 * <p>Two secure delivery modes are supported:
 * <ol>
 *   <li><b>stdin</b> — the caller pipes the token into the process:
 *       {@code echo "$TOKEN" | java -jar datev-helper.jar upload ...}</li>
 *   <li><b>temp file</b> — the caller writes the token to a temporary file and passes
 *       its path via {@code --token-file}. The file is deleted immediately after reading.</li>
 * </ol>
 *
 * <p>Neither mode exposes the token in the OS process list or shell history.
 */
public final class TokenReader {

    private TokenReader() {}

    /**
     * Reads the token from stdin.
     * Reads the first non-blank line; trims leading and trailing whitespace.
     *
     * @throws CliException if stdin contains no non-blank line
     */
    public static String readFromStdin() throws CliException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String token = line.strip();
                if (!token.isEmpty()) return token;
            }
        } catch (IOException e) {
            throw new CliException("Failed to read token from stdin: " + e.getMessage());
        }
        throw new CliException("No token found on stdin (expected a non-empty Bearer token)");
    }

    /**
     * Reads the token from a file and <b>deletes the file immediately</b> after reading.
     *
     * <p>The file must contain exactly one non-blank line with the token.
     *
     * @param path path to the token file
     * @throws CliException if the file cannot be read or contains no non-blank line
     */
    public static String readFromFile(Path path) throws CliException {
        if (!Files.exists(path)) {
            throw new CliException("Token file not found: " + path);
        }

        String token;
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8).strip();
            token = content.lines()
                    .map(String::strip)
                    .filter(l -> !l.isEmpty())
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new CliException("Failed to read token file '" + path + "': " + e.getMessage());
        } finally {
            // Always attempt deletion, even if reading failed.
            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
        }

        if (token == null || token.isEmpty()) {
            throw new CliException("Token file '" + path + "' is empty");
        }
        return token;
    }
}
