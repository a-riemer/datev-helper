package de.bewidata.datev.xmlonline.cli;

import de.bewidata.datev.xmlonline.DATEVXmlConfig;
import de.bewidata.datev.xmlonline.DATEVXmlProcessor;
import de.bewidata.datev.xmlonline.DocumentTooLargeException;
import de.bewidata.datev.xmlonline.model.BookingLineWithDocuments;
import de.bewidata.datev.xmlonline.plugin.FindStatus;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentException;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentProvider;
import de.bewidata.datev.xmlonline.split.BatchSplitStrategy;
import de.bewidata.datev.xmlonline.split.ByDocumentTypeSplitStrategy;
import de.bewidata.datev.xmlonline.split.SequentialSplitStrategy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Subcommand {@code providedocuments}: reads an EXTF booking batch CSV, resolves
 * source documents via a {@link SourceDocumentProvider}, and writes DATEV-compatible
 * ZIP archives alongside a separate EXTF CSV.
 *
 * <p>Example with the built-in provider (for testing):
 * <pre>
 *   java -jar datev-helper-cli.jar providedocuments --example \
 *       --consultant-number 12345 --client-number 1 \
 *       buchungsstapel.csv output/
 * </pre>
 *
 * <p>Example with a custom provider loaded from an external JAR:
 * <pre>
 *   java -jar datev-helper-cli.jar providedocuments \
 *       --provider-class com.example.MyDocumentProvider \
 *       --provider-jar  /path/to/my-provider.jar \
 *       buchungsstapel.csv output/
 * </pre>
 */
class ProvideDocumentsCommand implements Command {

    private static String buildUsage() {
        return String.join(System.lineSeparator(),
            "Usage: java -jar " + DATEVXmlCli.jarName() + " providedocuments [options] <input-csv> <output-dir>",
            "",
            "Arguments:",
            "  <input-csv>                Path to the EXTF booking batch CSV file",
            "  <output-dir>               Output directory for the ZIP archive(s)",
            "",
            "Options:",
            "  --consultant-number N      DATEV consultant number (Beraternummer)",
            "  --client-number N          DATEV client number (Mandantennummer)",
            "  --client-name NAME         DATEV client name (Mandantenname, max 36 chars)",
            "  --description TEXT         Description for the archive",
            "  --strategy by-type|sequential  Split strategy (default: by-type)",
            "  --provider-class CLASSNAME Fully qualified class name of a SourceDocumentProvider",
            "  --provider-jar PATH        JAR file containing the provider class (optional)",
            "  --provider-param KEY=VALUE Parameter passed to the provider's configure() method",
            "                             (can be specified multiple times)",
            "  --example                  Use built-in example document provider (for testing)",
            "  --verbose, -v              Print document find status for each booking line",
            "  --help, -h                 Show this help"
        );
    }

    @Override
    public String name() { return "providedocuments"; }

    @Override
    public String synopsis() { return "Process an EXTF booking batch and attach source documents"; }

    @Override
    public int execute(String[] args) {
        ParsedArgs parsed;
        try {
            parsed = parse(args);
        } catch (CliException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(buildUsage());
            return 1;
        }

        if (parsed.help) {
            System.out.println(buildUsage());
            return 0;
        }

        SourceDocumentProvider provider;
        try {
            provider = ProviderLoader.load(parsed.example, parsed.providerClass, parsed.providerJar);
            provider.configure(parsed.providerParams);
        } catch (CliException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(buildUsage());
            return 1;
        } catch (Exception e) {
            System.err.println("Error initialising document provider: " + e.getMessage());
            printCauses(e.getCause());
            return 1;
        }
        provider.configSummary().ifPresent(System.out::println);

        DATEVXmlConfig.Builder configBuilder = DATEVXmlConfig.builder();
        if (parsed.consultantNumber != null) configBuilder.consultantNumber(parsed.consultantNumber);
        if (parsed.clientNumber     != null) configBuilder.clientNumber(parsed.clientNumber);
        if (parsed.clientName       != null) configBuilder.clientName(parsed.clientName);
        if (parsed.description      != null) configBuilder.description(parsed.description);
        DATEVXmlConfig config = configBuilder.build();

        BatchSplitStrategy strategy = "sequential".equals(parsed.strategy)
                ? new SequentialSplitStrategy()
                : new ByDocumentTypeSplitStrategy();

        DATEVXmlProcessor processor = new DATEVXmlProcessor(config, provider, strategy);
        if (parsed.verbose) {
            processor.withLineListener(ProvideDocumentsCommand::printVerboseLine);
        }
        processor.withHintListener(message -> System.out.println("Hint: " + message));

        System.out.println("Processing: " + parsed.inputCsv);
        System.out.println("Output:     " + parsed.outputDir);

        List<Path> zips;
        try {
            zips = processor.process(Path.of(parsed.inputCsv), Path.of(parsed.outputDir));
        } catch (DocumentTooLargeException e) {
            System.err.printf("Error: Document exceeds size limit (%.1f MB > %.1f MB limit): %s%n",
                    e.getSize() / 1_048_576.0, e.getLimit() / 1_048_576.0, e.getPath());
            return 1;
        } catch (SourceDocumentException e) {
            System.err.println("Error: " + e.getMessage());
            printCauses(e.getCause());
            return 1;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            printCauses(e.getCause());
            return 1;
        }

        System.out.printf("Done. %d ZIP archive(s) created:%n", zips.size());
        for (Path zip : zips) {
            System.out.println("  " + zip.toAbsolutePath());
        }
        return 0;
    }

    // ---- verbose output ----

    private static void printVerboseLine(BookingLineWithDocuments lwd) {
        String statusLabel = switch (lwd.findStatus()) {
            case FOUND     -> "FOUND    ";
            case NOT_FOUND -> "NOT_FOUND";
            case SKIPPED   -> "SKIPPED  ";
        };
        String detail = lwd.getFindMessage().orElse(
                lwd.findStatus() == FindStatus.FOUND
                        ? lwd.documents().size() + " document(s)"
                        : "");
        System.out.printf("  [line %4d] %s  %s%n",
                lwd.line().getLineNumber(), statusLabel, detail);
    }

    // ---- error formatting ----

    private static void printCauses(Throwable t) {
        String lastMessage = null;
        while (t != null) {
            String msg = t.getClass().getSimpleName()
                    + (t.getMessage() != null ? ": " + t.getMessage() : "");
            if (!msg.equals(lastMessage)) {
                System.err.println("  caused by: " + msg);
                lastMessage = msg;
            }
            t = t.getCause();
        }
    }

    // ---- argument parsing ----

    private static ParsedArgs parse(String[] args) throws CliException {
        ParsedArgs result = new ParsedArgs();
        List<String> positional = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help", "-h"        -> result.help = true;
                case "--example"           -> result.example = true;
                case "--verbose", "-v"     -> result.verbose = true;
                case "--consultant-number" -> result.consultantNumber = requireLong(args, i++);
                case "--client-number"     -> result.clientNumber = requireLong(args, i++);
                case "--client-name"       -> result.clientName = requireString(args, i++);
                case "--description"       -> result.description = requireString(args, i++);
                case "--provider-class"    -> result.providerClass = requireString(args, i++);
                case "--provider-jar"      -> result.providerJar = requireString(args, i++);
                case "--provider-param"    -> {
                    String kv = requireString(args, i++);
                    int eq = kv.indexOf('=');
                    if (eq <= 0) {
                        throw new CliException(
                            "--provider-param must be in the form KEY=VALUE, got: " + kv);
                    }
                    result.providerParams.put(kv.substring(0, eq), kv.substring(eq + 1));
                }
                case "--strategy"          -> {
                    result.strategy = requireString(args, i++);
                    if (!"by-type".equals(result.strategy) && !"sequential".equals(result.strategy)) {
                        throw new CliException("Unknown strategy: " + result.strategy
                                + " (expected: by-type or sequential)");
                    }
                }
                default -> {
                    if (args[i].startsWith("--")) {
                        throw new CliException("Unknown option: " + args[i]);
                    }
                    positional.add(args[i]);
                }
            }
        }

        if (!result.help) {
            if (positional.size() < 2) {
                throw new CliException("Missing required arguments: <input-csv> <output-dir>");
            }
            result.inputCsv  = positional.get(0);
            result.outputDir = positional.get(1);
        }
        return result;
    }

    private static String requireString(String[] args, int flagIndex) throws CliException {
        if (flagIndex + 1 >= args.length) {
            throw new CliException("Option " + args[flagIndex] + " requires a value");
        }
        return args[flagIndex + 1];
    }

    private static long requireLong(String[] args, int flagIndex) throws CliException {
        String value = requireString(args, flagIndex);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CliException("Option " + args[flagIndex] + " expects a number, got: " + value);
        }
    }

    private static final class ParsedArgs {
        boolean             help;
        boolean             example;
        boolean             verbose;
        String              inputCsv;
        String              outputDir;
        Long                consultantNumber;
        Long                clientNumber;
        String              clientName;
        String              description;
        String              strategy      = "by-type";
        String              providerClass;
        String              providerJar;
        Map<String, String> providerParams = new LinkedHashMap<>();
    }
}
