package de.bewidata.datev.xmlonline.cli;

/** Signals a user-facing CLI usage error (wrong option, missing argument, etc.). */
class CliException extends Exception {
    CliException(String message) { super(message); }
}
