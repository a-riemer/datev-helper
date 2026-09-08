package de.bewidata.datev.xmlonline.cli;

import de.bewidata.datev.xmlonline.example.ExampleDocumentProvider;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentProvider;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * Shared provider-loading logic for CLI commands that accept a {@link SourceDocumentProvider}.
 * Package-private — used by {@link ProvideDocumentsCommand} and {@link DATEVUploadCommand}.
 */
final class ProviderLoader {

    private ProviderLoader() {}

    /**
     * Instantiates and returns a {@link SourceDocumentProvider}.
     *
     * @param example       use the built-in {@link ExampleDocumentProvider}
     * @param providerClass fully qualified class name of a custom provider, or {@code null}
     * @param providerJar   path to a JAR containing the provider class, or {@code null}
     * @throws CliException if the arguments are inconsistent or the class cannot be loaded
     * @throws Exception    if the provider constructor throws
     */
    static SourceDocumentProvider load(boolean example, String providerClass, String providerJar)
            throws CliException, Exception {

        if (example && providerClass != null) {
            throw new CliException("--example and --provider-class cannot be combined");
        }
        if (example) {
            System.out.println("Using built-in ExampleDocumentProvider (for testing only).");
            return new ExampleDocumentProvider();
        }
        if (providerClass != null) {
            return loadClass(providerClass, providerJar);
        }
        throw new CliException(
            "No document provider specified. Use --example for testing, or supply "
            + "--provider-class (and optionally --provider-jar) for a real implementation.");
    }

    private static SourceDocumentProvider loadClass(String className, String jarPath)
            throws CliException, Exception {

        ClassLoader cl = buildClassLoader(jarPath);
        Class<?> cls;
        try {
            cls = Class.forName(className, true, cl);
        } catch (ClassNotFoundException e) {
            String hint = jarPath == null
                ? " (is it on the classpath? Consider --provider-jar)"
                : " (is the class name correct and the class inside " + jarPath + "?)";
            throw new CliException("Provider class not found: " + className + hint);
        }
        if (!SourceDocumentProvider.class.isAssignableFrom(cls)) {
            throw new CliException(className + " does not implement SourceDocumentProvider");
        }
        try {
            return (SourceDocumentProvider) cls.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new CliException(className + " must have a public no-argument constructor");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new Exception("Constructor of " + className + " threw an exception: "
                    + cause.getMessage(), cause);
        }
    }

    private static ClassLoader buildClassLoader(String jarPath) throws CliException {
        if (jarPath == null) {
            return ProviderLoader.class.getClassLoader();
        }
        Path jar = Path.of(jarPath);
        if (!jar.toFile().isFile()) {
            throw new CliException("Provider JAR not found: " + jarPath);
        }
        try {
            URL jarUrl = jar.toUri().toURL();
            return new URLClassLoader(new URL[]{jarUrl}, ProviderLoader.class.getClassLoader());
        } catch (Exception e) {
            throw new CliException("Cannot load provider JAR: " + e.getMessage());
        }
    }
}
