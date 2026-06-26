package org.sparkcorporation.lims.result.infra.pdf;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Pure-data model for the result compte-rendu (CR) PDF. Built by {@link ResultReportModelAssembler}
 * from cross-module {@code *Api} snapshots; consumed by {@code result/cr.html} via {@code
 * OpenHtmlToPdfRenderingService}.
 *
 * <p>All fields are denormalized {@code String}s (or basic value types) so the template never
 * touches an entity. Nullable references — rather than {@code Optional} — are used for fields that
 * may be absent at render time: the Jackson-to-Map step in {@link
 * org.sparkcorporation.lims.pdf.infra.OpenHtmlToPdfRenderingService} unwraps {@code Optional} via
 * {@code Jdk8Module}, so the template would receive either the underlying value or {@code null}
 * regardless. Nullable references make the template's {@code != null} guard the canonical form.
 *
 * <p>The lab identity block is carried on the model itself — there is no {@code LabSettings}
 * aggregate in this codebase yet (see Chunk 6 deviation note). A spec amendment in chunk 7 / a
 * follow-up sub-project will introduce a real {@code LabSettings} VO and replace the hardcoded
 * default below; for now the assembler synthesizes a placeholder.
 */
public record ResultReportModel(
        LabFileSummary labFile,
        PatientSummary patient,
        @Nullable PractitionerSummary requestingPractitioner,
        LabIdentity labIdentity,
        Instant biologicalValidatedAt,
        String biologicalValidatedAtDisplay,
        UserSummary biologicalValidatedBy,
        @Nullable String signedByDisplay,
        @Nullable String signedAtDisplay,
        List<AnalysisGroupBlock> groups,
        List<AntibiogramBlock> antibiograms,
        Locale locale) {

    public ResultReportModel {
        groups = List.copyOf(groups);
        antibiograms = List.copyOf(antibiograms);
    }

    /**
     * Identifies the lab file the CR is rendered for. {@code dailyId} is the registration number.
     */
    public record LabFileSummary(String labFileId, String dailyId, Instant openedAt) {}

    /**
     * Patient identity block for the header. All values pre-formatted (sex letter, French dates,
     * computed age display) so the template renders directly.
     */
    public record PatientSummary(
            String patientId,
            String firstName,
            String lastName,
            String sex,
            String dateOfBirth,
            String ageDisplay) {}

    /** Requesting prescriber (CDC §II — "médecin prescripteur"). */
    public record PractitionerSummary(String practitionerId, String displayName, String title) {}

    /**
     * Lab identity block — header + footer. Placeholder until a {@code LabSettings} aggregate
     * lands.
     */
    public record LabIdentity(
            String name, String address, String accreditationNumber, String managerName) {}

    /** Biologist who signed the final biological validation. */
    public record UserSummary(String userId, String displayName, String title) {}

    /** One paillasse (discipline) block: all rows from analyses belonging to that paillasse. */
    public record AnalysisGroupBlock(String paillasseName, List<AnalysisRow> rows) {
        public AnalysisGroupBlock {
            rows = List.copyOf(rows);
        }
    }

    /**
     * One row per result line. {@code outOfRange} drives the red styling in the template. Unit-1
     * fields ({@code valueDisplay}, {@code unit}, {@code referenceRange}) are always present; the
     * unit-2 fields ({@code value2}, {@code unit2}, {@code referenceRange2}) are {@code null} when
     * the analysis has no second unit configured. {@code interpretation} is set only on the first
     * row of an analysis, and only when the analysis has non-blank interpretation text (CDC
     * §Config.8 NB — printed only when filled).
     */
    public record AnalysisRow(
            String code,
            String label,
            String valueDisplay,
            String unit,
            @Nullable String value2,
            @Nullable String unit2,
            String referenceRange,
            @Nullable String referenceRange2,
            boolean outOfRange,
            @Nullable String comment,
            @Nullable String interpretation) {}

    /** Antibiogram block — one per Result with antibiogram readings. */
    public record AntibiogramBlock(String sampleLabel, List<AntibiogramReading> readings) {
        public AntibiogramBlock {
            readings = List.copyOf(readings);
        }
    }

    /** One reading per antibiotic tested on a culture. */
    public record AntibiogramReading(
            String antibioticSigle, String diameterMm, String interpretation) {}
}
