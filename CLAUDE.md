# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Java 17 Maven project — a collection of libraries and command-line tools for **DATEV Rechnungswesen** and **DATEV Unternehmen Online (DUO)**.

- **Group ID**: `de.bewidata`
- **Artifact ID**: `datev-helper`
- **Entry point**: `de.bewidata.Main`

## Build and Run

```bash
# Compile
mvn clean compile

# Build JAR
mvn clean package

# Run (requires exec-maven-plugin in pom.xml)
mvn exec:java -Dexec.mainClass="de.bewidata.Main"

# Run tests (none exist yet)
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName
```

## Design Principle

All classes are designed as **reusable library code** for other Java projects first. CLI tools are a secondary addition for selected functionality and always call into the library — never the other way around.

Library code must not use `System.exit()`, hardcoded paths, or other CLI-specific patterns.

## Architecture

### Package structure `de.bewidata.datev.xmlonline`

```
DATEVXmlProcessor       — main API (entry point)
DATEVHelperFacade       — simple-type facade for calling from InterSystems IRIS (Java External Server)
DATEVXmlConfig          — configuration (consultant number, client, size limits)
DocumentTooLargeException

extf/
  ExtfParser            — reads EXTF CSV (line 1 = metadata, line 2 = header, from line 3 = data)
  ExtfWriter            — writes EXTF CSV back out (adds the document-link column if missing)

model/
  BookingBatch          — the whole EXTF batch (metadata + all lines)
  BookingLine           — a single booking line with Map<column name, value>; setDocumentLink() sets "BEDI" + UUID
  SourceDocument        — a physical source document (builder pattern; incoming()/outgoing() as factory methods)
  BookingLineWithDocuments — record: a booking line plus its source documents

plugin/
  SourceDocumentProvider — @FunctionalInterface; implemented by the caller (app-specific)
  SourceDocumentException

split/
  BatchSplitStrategy         — interface for domain-specific grouping
  ByDocumentTypeSplitStrategy — splits by document type 1/2 (default)
  SequentialSplitStrategy    — no grouping, everything in one group

xml/
  DocumentXmlBuilder    — builds document.xml via JAXB; sets document links as a side effect

zip/
  ZipArchiveBuilder     — packs CSV + document.xml + source documents; splits when > 100 MB

jaxb/                   — JAXB model for document.xml (schema v6.0)
  Archive, Header, Content, Document
  Extension (abstract), FileExtension, InvoiceExtension
  Repository, RepositoryLevel, LocalDateTimeAdapter
```

### Data flow

1. `ExtfParser` reads the EXTF CSV → `BookingBatch`
2. Per `BookingLine` → `SourceDocumentProvider.findDocuments()` → `List<SourceDocument>`
3. Size validation: each document ≤ 20 MB (configurable)
4. `BatchSplitStrategy.split()` → domain-specific groups
5. Per group: `ZipArchiveBuilder.buildZips()` → split further sequentially on overflow (> 100 MB)
6. Per sub-group: generate UUID → set document link in the CSV → `DocumentXmlBuilder.build()` → write ZIP

### Key conventions

- **Document link format**: `BEDI` + UUID (uppercase, with hyphens), e.g. `BEDIAB12CD34-...`
  This value appears in the CSV column "Beleglink" and as the `guid` attribute on the `<document>` element.
- **XSD files**: `src/main/resources/xsd/` — reference for Document_v060.xsd (namespace `http://xml.datev.de/bedi/tps/document/v06.0`)
- **JAXB**: classes are hand-written (no Maven plugin), based on the XSDs

### Dependencies

- `jakarta.xml.bind-api:4.0.2` + `jaxb-runtime:4.0.5` — XML serialization
- `commons-csv:1.10.0` — EXTF CSV parsing

No testing framework is configured in `pom.xml` yet — add JUnit Jupiter before writing tests.