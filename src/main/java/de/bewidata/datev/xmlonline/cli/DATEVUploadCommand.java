package de.bewidata.datev.xmlonline.cli;

import de.bewidata.datev.acsapi.DATEVAcsApiConfig;
import de.bewidata.datev.acsapi.DATEVAcsApiException;
import de.bewidata.datev.acsapi.DATEVApiProcessor;
import de.bewidata.datev.acsapi.model.DocumentUploadMetadata;
import de.bewidata.datev.acsapi.model.UploadGroupResult;
import de.bewidata.datev.xmlonline.DocumentTooLargeException;
import de.bewidata.datev.xmlonline.model.BookingLineWithDocuments;
import de.bewidata.datev.xmlonline.plugin.FindStatus;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentException;
import de.bewidata.datev.xmlonline.plugin.SourceDocumentProvider;
import de.bewidata.datev.xmlonline.split.ByMonthSplitStrategy;
import de.bewidata.datev.xmlonline.split.SequentialSplitStrategy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Subcommand {@code upload}: resolves documents via a {@link SourceDocumentProvider},
 * uploads them together with the EXTF CSV to DATEV via the Buchungsdatenservice REST API,
 * and polls the import job(s) to completion.
 *
 * <p>Supports the same provider options as {@code providedocuments} — any
 * {@link SourceDocumentProvider} implementation works in both commands.
 *
 * <p>The OAuth Bearer token is read from stdin (default) or from a temporary file
 * ({@code --token-file}). The file is deleted immediately after reading.
 *
 * <p>By default, the booking batch is split by accounting month ({@code --strategy by-month}).
 * Each month group is uploaded as a separate EXTF import job. The command returns immediately
 * after all jobs have been submitted — it does <em>not</em> wait for DATEV to finish processing.
 * Use the {@code poll} command to check job status and retrieve error details.
 *
 * <p>Output: one line per group in the format {@code JOB <register> <jobId>}.
 *
 * <p>Exit codes:
 * <ul>
 *   <li>0 — all jobs submitted successfully</li>
 *   <li>1 — error (I/O, configuration, provider failure, HTTP error)</li>
 *   <li>2 — authentication failure (HTTP 401) — caller should trigger re-login</li>
 * </ul>
 */
class DATEVUploadCommand implements Command {

    private static String buildUsage() {
        String jar = DATEVXmlCli.jarName();
        return String.join(System.lineSeparator(),
            "Usage: java -jar " + jar + " upload [options] <input-csv>",
            "",
            "Arguments:",
            "  <input-csv>                EXTF CSV file (Buchungsstapel)",
            "",
            "Token (exactly one, required):",
            "  --token-file PATH          Read Bearer token from file (file is deleted after reading)",
            "  (default)                  Read Bearer token from stdin",
            "",
            "Document provider (exactly one, required):",
            "  --provider-class CLASSNAME Fully qualified class name of a SourceDocumentProvider",
            "  --provider-jar PATH        JAR file containing the provider class (optional)",
            "  --provider-param KEY=VALUE Parameter passed to the provider's configure() method",
            "                             (can be specified multiple times)",
            "  --example                  Use built-in example document provider (for testing)",
            "",
            "DATEV connection:",
            "  --app-client-id ID         OAuth App Client ID (X-DATEV-Client-Id header) [required]",
            "  --datev-client-id ID       DATEV Mandant ID, e.g. 455148-1",
            "                             (derived from EXTF CSV header if not specified)",
            "  --sandbox                  Use DATEV sandbox environment",
            "",
            "Document archive metadata:",
            "  --category TEXT            Document category (default: datev-helper)",
            "  --folder TEXT              Archive folder name (default: Belege)",
            "  --register YYYY-MM         Accounting period, e.g. 2025-01",
            "                             (derived from Belegdatum per group if not specified)",
            "",
            "Split strategy:",
            "  --strategy by-month        Split by accounting month; register derived automatically",
            "                             (default)",
            "  --strategy sequential      Upload the whole file as one job; use with --register",
            "",
            "Other:",
            "  --verbose, -v              Print document resolution status for each booking line",
            "  --help, -h                 Show this help",
            "",
            "Output: one line per group: JOB <register> <jobId>",
            "Use the 'poll' command to check job status."
        );
    }

    @Override
    public String name() { return "upload"; }

    @Override
    public String synopsis() {
        return "Resolve documents via provider and upload EXTF batch to DATEV REST API";
    }

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
        if (parsed.help) { System.out.println(buildUsage()); return 0; }

        // 1. Read token
        String token;
        try {
            token = parsed.tokenFile != null
                    ? TokenReader.readFromFile(Path.of(parsed.tokenFile))
                    : TokenReader.readFromStdin();
        } catch (CliException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }

        // 2. Load + configure provider
        SourceDocumentProvider provider;
        try {
            provider = ProviderLoader.load(parsed.example, parsed.providerClass, parsed.providerJar);
            provider.configure(parsed.providerParams);
        } catch (CliException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Error initialising document provider: " + e.getMessage());
            printCauses(e.getCause());
            return 1;
        }
        provider.configSummary().ifPresent(System.out::println);

        // 3. Build API config
        DATEVAcsApiConfig apiConfig;
        try {
            apiConfig = DATEVAcsApiConfig.builder()
                    .token(token)
                    .appClientId(parsed.appClientId)
                    .datevClientId(parsed.datevClientId)
                    .sandbox(parsed.sandbox)
                    .build();
        } catch (IllegalStateException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }

        // register null → DATEVApiProcessor derives it per group from Belegdatum
        DocumentUploadMetadata meta = new DocumentUploadMetadata(
                parsed.category, parsed.folder, parsed.register);

        DATEVApiProcessor processor = new DATEVApiProcessor(apiConfig, provider, meta);
        if (parsed.verbose) {
            processor.withLineListener(DATEVUploadCommand::printVerboseLine);
        }
        if ("sequential".equals(parsed.strategy)) {
            processor.withSplitStrategy(new SequentialSplitStrategy());
        } else {
            processor.withSplitStrategy(new ByMonthSplitStrategy());
        }

        // 4. Upload — one job per group; return immediately without polling
        System.err.println("Processing: " + parsed.inputCsv);
        List<UploadGroupResult> results;
        try {
            results = processor.upload(Path.of(parsed.inputCsv));
        } catch (DATEVAcsApiException e) {
            System.err.println("Error: " + e.getMessage());
            if (e.getResponseBody() != null && !e.getResponseBody().isBlank()) {
                System.err.println("  Response: " + e.getResponseBody());
            }
            if (e.isUnauthorized()) return 2;
            return 1;
        } catch (DocumentTooLargeException e) {
            System.err.printf("Error: Document exceeds size limit (%.1f MB > %.1f MB): %s%n",
                    e.getSize() / 1_048_576.0, e.getLimit() / 1_048_576.0, e.getPath());
            return 1;
        } catch (SourceDocumentException e) {
            System.err.println("Error: " + e.getMessage());
            printCauses(e.getCause());
            return 1;
        } catch (IOException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }

        if (results.isEmpty()) {
            System.err.println("No booking lines to process.");
            return 0;
        }

        // Output one parseable line per group; progress/errors go to stderr
        for (UploadGroupResult r : results) {
            System.out.println("JOB " + r.register() + " " + r.jobId());
        }
        return 0;
    }

    // ---- verbose output ----

    private static void printVerboseLine(BookingLineWithDocuments lwd) {
        String label = switch (lwd.findStatus()) {
            case FOUND     -> "FOUND    ";
            case NOT_FOUND -> "NOT_FOUND";
            case SKIPPED   -> "SKIPPED  ";
        };
        String detail = lwd.getFindMessage().orElse(
                lwd.findStatus() == FindStatus.FOUND
                        ? lwd.documents().size() + " document(s)"
                        : "");
        System.out.printf("  [line %4d] %s  %s%n", lwd.line().getLineNumber(), label, detail);
    }

    // ---- error formatting ----

    private static void printCauses(Throwable t) {
        String lastMsg = null;
        while (t != null) {
            String msg = t.getClass().getSimpleName()
                    + (t.getMessage() != null ? ": " + t.getMessage() : "");
            if (!msg.equals(lastMsg)) { System.err.println("  caused by: " + msg); lastMsg = msg; }
            t = t.getCause();
        }
    }

    // ---- argument parsing ----

    private static ParsedArgs parse(String[] args) throws CliException {
        ParsedArgs result = new ParsedArgs();
        List<String> positional = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help", "-h"      -> result.help          = true;
                case "--example"         -> result.example       = true;
                case "--verbose", "-v"   -> result.verbose       = true;
                case "--sandbox"         -> result.sandbox       = true;
                case "--token-file"      -> result.tokenFile     = requireString(args, i++);
                case "--app-client-id"   -> result.appClientId   = requireString(args, i++);
                case "--datev-client-id" -> result.datevClientId = requireString(args, i++);
                case "--provider-class"  -> result.providerClass = requireString(args, i++);
                case "--provider-jar"    -> result.providerJar   = requireString(args, i++);
                case "--category"        -> result.category      = requireString(args, i++);
                case "--folder"          -> result.folder        = requireString(args, i++);
                case "--register"        -> result.register      = requireString(args, i++);
                case "--strategy"        -> {
                    result.strategy = requireString(args, i++);
                    if (!"by-month".equals(result.strategy) && !"sequential".equals(result.strategy)) {
                        throw new CliException("Unknown strategy: " + result.strategy
                                + " (expected: by-month or sequential)");
                    }
                }
                case "--provider-param"  -> {
                    String kv = requireString(args, i++);
                    int eq = kv.indexOf('=');
                    if (eq <= 0) throw new CliException(
                        "--provider-param must be KEY=VALUE, got: " + kv);
                    result.providerParams.put(kv.substring(0, eq), kv.substring(eq + 1));
                }
                default -> {
                    if (args[i].startsWith("--")) throw new CliException("Unknown option: " + args[i]);
                    positional.add(args[i]);
                }
            }
        }

        if (!result.help) {
            if (result.appClientId == null) throw new CliException("--app-client-id is required");
            if (positional.isEmpty())       throw new CliException("Missing required argument: <input-csv>");
            result.inputCsv = positional.get(0);
        }
        return result;
    }

    private static String requireString(String[] args, int i) throws CliException {
        if (i + 1 >= args.length) throw new CliException("Option " + args[i] + " requires a value");
        return args[i + 1];
    }

    private static int requireInt(String[] args, int i) throws CliException {
        String val = requireString(args, i);
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) {
            throw new CliException("Option " + args[i] + " expects a number, got: " + val);
        }
    }

    private static final class ParsedArgs {
        boolean             help;
        boolean             example;
        boolean             verbose;
        boolean             sandbox;
        String              tokenFile;
        String              appClientId;
        String              datevClientId;
        String              providerClass;
        String              providerJar;
        String              category           = "datev-helper";
        String              folder             = "Belege";
        String              register;
        String              strategy           = "by-month";
        String              inputCsv;
        Map<String, String> providerParams     = new LinkedHashMap<>();
    }
}
