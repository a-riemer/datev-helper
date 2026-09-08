# DATEV Helper

A Java 17 library and set of command-line tools for **DATEV Rechnungswesen** and **DATEV Unternehmen Online (DUO)**.

## Build and Run

```bash
# Build the shaded CLI JAR
mvn clean package

# Show CLI usage
java -jar target/datev-helper-1.0-SNAPSHOT-cli.jar --help
```

## Design Principle

All classes are designed as reusable library code first; the CLI tools are a secondary layer that calls into the library.

## Architecture

See [CLAUDE.md](CLAUDE.md) for the package structure, data flow, and key conventions.
