package de.bewidata.datev.acsapi.model;

/**
 * Result of uploading one booking batch group via the Buchungsdatenservice REST API.
 *
 * <p>A booking batch may be split into multiple groups (e.g. by accounting month).
 * Each group is uploaded as a separate EXTF import job and produces one result.
 *
 * @param jobId    DATEV import job ID — use with
 *                 {@link de.bewidata.datev.acsapi.AccountingExtfFilesClient#awaitCompletion}
 *                 or the {@code poll} CLI command
 * @param register accounting period derived from this group's booking lines (e.g. {@code "2025-01"}),
 *                 or the explicitly configured register if one was set
 */
public record UploadGroupResult(String jobId, String register) {}
