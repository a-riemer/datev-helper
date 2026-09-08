package de.bewidata.datev.xmlonline.cli;

/**
 * A CLI subcommand that can be dispatched by {@link DATEVXmlCli}.
 *
 * <p>Implementations receive the arguments <em>after</em> the command name,
 * i.e. {@code args[0]} is the first option or positional argument, not the
 * command name itself.
 */
interface Command {
    /** Subcommand name as typed on the command line (e.g. {@code "providedocuments"}). */
    String name();

    /** One-line description shown in the top-level help. */
    String synopsis();

    /**
     * Executes the command.
     *
     * @param args arguments after the command name
     * @return exit code (0 = success, non-zero = failure)
     */
    int execute(String[] args);
}
