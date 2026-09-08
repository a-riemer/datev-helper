package de.bewidata.datev.xmlonline.cli;

import de.bewidata.datev.xmlonline.DATEVHelperVersion;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point for the DATEV helper CLI. Dispatches to the appropriate subcommand.
 *
 * <p>Usage:
 * <pre>
 *   java -jar datev-helper-cli.jar &lt;command&gt; [options]
 * </pre>
 *
 * <p>Run {@code --help} to list available commands, or
 * {@code <command> --help} for command-specific options.
 */
public class DATEVXmlCli {

    private static final List<Command> COMMANDS = List.of(
        new ProvideDocumentsCommand(),
        new DATEVUploadCommand(),
        new PollCommand()
    );

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /**
     * Runs the CLI without calling {@link System#exit}, suitable for testing.
     *
     * @param args command-line arguments
     * @return exit code (0 = success, non-zero = error)
     */
    public static int run(String[] args) {
        if (args.length == 0) {
            System.err.println("Error: No command specified.");
            System.err.println();
            System.err.println(buildUsage());
            return 1;
        }

        switch (args[0]) {
            case "--help", "-h" -> { System.out.println(buildUsage()); return 0; }
            case "--version", "-V" -> {
                System.out.println("datev-helper " + DATEVHelperVersion.getVersion());
                return 0;
            }
        }

        String name = args[0];
        for (Command cmd : COMMANDS) {
            if (cmd.name().equals(name)) {
                return cmd.execute(Arrays.copyOfRange(args, 1, args.length));
            }
        }

        System.err.println("Error: Unknown command: " + name);
        System.err.println();
        System.err.println(buildUsage());
        return 1;
    }

    private static String buildUsage() {
        String jar = jarName();
        int nameWidth = COMMANDS.stream().mapToInt(c -> c.name().length()).max().orElse(10);
        StringBuilder sb = new StringBuilder();
        sb.append("Usage: java -jar ").append(jar).append(" <command> [options]").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("Commands:").append(System.lineSeparator());
        for (Command cmd : COMMANDS) {
            sb.append(String.format("  %-" + (nameWidth + 2) + "s %s%n",
                    cmd.name(), cmd.synopsis()));
        }
        sb.append(System.lineSeparator());
        sb.append("Options:").append(System.lineSeparator());
        sb.append("  --version, -V        Print version and exit").append(System.lineSeparator());
        sb.append("  --help, -h           Show this help").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("Run 'java -jar ").append(jar).append(" <command> --help' for command-specific options.");
        return sb.toString();
    }

    static String jarName() {
        try {
            java.net.URI uri = DATEVXmlCli.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            String name = Paths.get(uri).getFileName().toString();
            return name.endsWith(".jar") ? name : "datev-helper-cli.jar";
        } catch (URISyntaxException e) {
            return "datev-helper-cli.jar";
        }
    }
}
