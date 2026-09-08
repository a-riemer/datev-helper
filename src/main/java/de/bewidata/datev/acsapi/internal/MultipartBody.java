package de.bewidata.datev.acsapi.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Builds a {@code multipart/form-data} request body.
 * Package-private — use via the API client classes.
 */
public final class MultipartBody {

    private static final String CRLF = "\r\n";

    private final String              boundary;
    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

    public MultipartBody(String boundary) {
        this.boundary = boundary;
    }

    /** Adds a plain-text form field. */
    public MultipartBody addText(String name, String value, String contentType) throws IOException {
        line("--" + boundary);
        line("Content-Disposition: form-data; name=\"" + name + "\"");
        line("Content-Type: " + contentType);
        line("");
        line(value);
        return this;
    }

    /** Adds a binary file field. */
    public MultipartBody addFile(String name, String filename, Path path) throws IOException {
        line("--" + boundary);
        line("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"");
        line("Content-Type: application/octet-stream");
        line("");
        buf.write(Files.readAllBytes(path));
        line("");
        return this;
    }

    /** Finalises the body and returns the raw bytes. */
    public byte[] finish() throws IOException {
        line("--" + boundary + "--");
        return buf.toByteArray();
    }

    public String boundary() { return boundary; }

    public String contentTypeHeader() {
        return "multipart/form-data; boundary=" + boundary;
    }

    private void line(String s) throws IOException {
        buf.write((s + CRLF).getBytes(StandardCharsets.UTF_8));
    }
}
