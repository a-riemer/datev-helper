package de.bewidata.datev.xmlonline.cli;

import de.bewidata.datev.acsapi.AccountingExtfFilesClient;
import de.bewidata.datev.acsapi.DATEVAcsApiConfig;
import de.bewidata.datev.acsapi.DATEVAcsApiException;
import de.bewidata.datev.acsapi.model.ExtfJobStatus;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand {@code poll}: checks the status of one or more DATEV EXTF import jobs
 * and waits until each reaches a terminal state.
 *
 * <p>Job IDs are obtained from the output of the {@code upload} command
 * (lines in the format {@code JOB <register> <jobId>}).
 *
 * <p>By default, each job is checked <em>once</em> and the command returns immediately with the
 * current status — even if the job is still processing (status 1–3). The caller is responsible
 * for invoking {@code poll} again later to check progress.
 *
 * <p>With {@code --wait}, the command polls each job in a loop until it reaches a terminal state
 * or the timeout elapses. This is suitable for background jobs where blocking is acceptable.
 *
 * <p>For each job, one result line is written to stdout:
 * <pre>
 *   JOB &lt;jobId&gt; STATUS &lt;code&gt; PROCESSING
 *   JOB &lt;jobId&gt; STATUS &lt;code&gt; OK
 *   JOB &lt;jobId&gt; STATUS &lt;code&gt; ERROR &lt;datev-response-body&gt;
 * </pre>
 *
 * <p>Progress and error messages go to stderr so that stdout can be parsed by scripts.
 *
 * <p>Exit codes:
 * <ul>
 *   <li>0 — all checked jobs are either still processing or completed successfully</li>
 *   <li>1 — error (I/O, configuration, HTTP error during polling)</li>
 *   <li>2 — authentication failure (HTTP 401) — caller should trigger re-login</li>
 *   <li>3 — one or more jobs completed with DATEV validation errors</li>
 * </ul>
 */
class PollCommand implements Command {

    private static final int DEFAULT_TIMEOUT_SECONDS  = 300;
    private static final int DEFAULT_INTERVAL_SECONDS = 5;

    private static String buildUsage() {
        String jar = DATEVXmlCli.jarName();
        return String.join(System.lineSeparator(),
            "Usage: java -jar " + jar + " poll [options] <jobId> [<jobId> ...]",
            "",
            "Arguments:",
            "  <jobId>                    One or more DATEV import job IDs (from 'upload' output)",
            "",
            "Token (exactly one, required):",
            "  --token-file PATH          Read Bearer token from file (file is deleted after reading)",
            "  (default)                  Read Bearer token from stdin",
            "",
            "DATEV connection:",
            "  --app-client-id ID         OAuth App Client ID (X-DATEV-Client-Id header) [required]",
            "  --datev-client-id ID       DATEV Mandant ID, e.g. 455148-1 [required]",
            "  --sandbox                  Use DATEV sandbox environment",
            "",
            "Polling:",
            "  --wait                     Wait internally until each job reaches a terminal state",
            "                             (default: check once and return immediately)",
            "  --timeout SECONDS          Max seconds to wait per job, requires --wait",
            "                             (default: " + DEFAULT_TIMEOUT_SECONDS + ")",
            "  --interval SECONDS         Polling interval in seconds, requires --wait",
            "                             (default: " + DEFAULT_INTERVAL_SECONDS + ")",
            "",
            "Other:",
            "  --help, -h                 Show this help",
            "",
            "Output per job: JOB <jobId> STATUS <code> PROCESSING|OK|ERROR [<datev-response>]"
        );
    }

    @Override
    public String name() { return "poll"; }

    @Override
    public String synopsis() { return "Poll DATEV import job status until terminal"; }

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

        // Read token
        String token;
        try {
            token = parsed.tokenFile != null
                    ? TokenReader.readFromFile(java.nio.file.Path.of(parsed.tokenFile))
                    : TokenReader.readFromStdin();
        } catch (CliException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }

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

        AccountingExtfFilesClient client = new AccountingExtfFilesClient(
                apiConfig,
                Duration.ofSeconds(parsed.intervalSeconds));

        boolean anyErrors = false;
        for (String jobId : parsed.jobIds) {
            ExtfJobStatus status;
            try {
                if (parsed.wait) {
                    System.err.println("Waiting for job " + jobId + " (timeout: " + parsed.timeoutSeconds + "s)...");
                    status = client.awaitCompletion(jobId, Duration.ofSeconds(parsed.timeoutSeconds));
                } else {
                    status = client.getJobStatus(jobId);
                }
            } catch (DATEVAcsApiException e) {
                System.err.println("Error polling job " + jobId + ": " + e.getMessage());
                if (e.isUnauthorized()) return 2;
                return 1;
            } catch (IOException | InterruptedException e) {
                System.err.println("Error polling job " + jobId + ": " + e.getMessage());
                return 1;
            }

            if (!status.isTerminal()) {
                System.out.println("JOB " + jobId + " STATUS " + status.statusCode() + " PROCESSING");
            } else if (status.isSuccess()) {
                System.out.println("JOB " + jobId + " STATUS " + status.statusCode() + " OK");
            } else {
                String details = status.responseBody() != null ? status.responseBody() : "";
                System.out.println("JOB " + jobId + " STATUS " + status.statusCode() + " ERROR " + details);
                anyErrors = true;
            }
        }

        return anyErrors ? 3 : 0;
    }

    // ---- argument parsing ----

    private static ParsedArgs parse(String[] args) throws CliException {
        ParsedArgs result = new ParsedArgs();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help", "-h"      -> result.help          = true;
                case "--sandbox"         -> result.sandbox       = true;
                case "--wait"            -> result.wait          = true;
                case "--token-file"      -> result.tokenFile     = requireString(args, i++);
                case "--app-client-id"   -> result.appClientId   = requireString(args, i++);
                case "--datev-client-id" -> result.datevClientId = requireString(args, i++);
                case "--timeout"         -> result.timeoutSeconds  = requireInt(args, i++);
                case "--interval"        -> result.intervalSeconds = requireInt(args, i++);
                default -> {
                    if (args[i].startsWith("--")) throw new CliException("Unknown option: " + args[i]);
                    result.jobIds.add(args[i]);
                }
            }
        }

        if (!result.help) {
            if (result.appClientId == null)  throw new CliException("--app-client-id is required");
            if (result.datevClientId == null) throw new CliException("--datev-client-id is required");
            if (result.jobIds.isEmpty())     throw new CliException("At least one job ID is required");
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
        boolean      help;
        boolean      sandbox;
        boolean      wait;
        String       tokenFile;
        String       appClientId;
        String       datevClientId;
        int          timeoutSeconds  = DEFAULT_TIMEOUT_SECONDS;
        int          intervalSeconds = DEFAULT_INTERVAL_SECONDS;
        List<String> jobIds          = new ArrayList<>();
    }
}
