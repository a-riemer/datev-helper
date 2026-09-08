package de.bewidata.datev.xmlonline;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides the library version at runtime.
 *
 * <p>The version string is read from {@code version.properties}, which is populated
 * by Maven resource filtering during the build ({@code ${project.version}}).
 */
public final class DATEVHelperVersion {

    private static final String VERSION;

    static {
        String v = "unknown";
        try (InputStream is = DATEVHelperVersion.class
                .getResourceAsStream("version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                v = props.getProperty("version", "unknown");
            }
        } catch (IOException ignored) {
        }
        VERSION = v;
    }

    private DATEVHelperVersion() {}

    /** Returns the library version (e.g. {@code "1.0-SNAPSHOT"} or {@code "1.2.0"}). */
    public static String getVersion() {
        return VERSION;
    }
}
