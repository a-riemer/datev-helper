package de.bewidata;

import de.bewidata.datev.xmlonline.cli.DATEVXmlCli;

/**
 * Entry point for the DATEV helper command-line tools.
 *
 * <p>Delegates to {@link DATEVXmlCli} for the DATEV XML-Schnittstelle online tool.
 */
public class Main {
    public static void main(String[] args) {
        DATEVXmlCli.main(args);
    }
}
