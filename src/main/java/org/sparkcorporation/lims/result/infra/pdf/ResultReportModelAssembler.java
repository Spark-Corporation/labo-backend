package org.sparkcorporation.lims.result.infra.pdf;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.AntibioticApi;
import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.AntibioticSummary;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;
import org.sparkcorporation.lims.iam.api.UserApi;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.iam.api.UserSummary;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.api.ResultSummary;

/**
 * Builds a {@link ResultReportModel} for the compte-rendu (CR) PDF from cross-module {@code *Api}
 * snapshots. Stateless, transactional read-only.
 *
 * <p>Contract:
 *
 * <ul>
 *   <li>Throws {@code LabFileNotFoundException} (propagated from {@link LabFileApi#requireById})
 *       when the lab file is unknown — mapped to 404 by the shared advice.
 *   <li>Throws {@link ResultsNotValidatedException} when any result on the lab file is not yet in
 *       {@link ResultState#BIOLOGICAL_VALIDATED} — mapped to 409 by {@code
 *       ResultProblemDetailAdvice}.
 *   <li>Returns a fully-denormalized {@link ResultReportModel} otherwise; no {@code Optional} on
 *       template-bound fields (Jackson's {@code Jdk8Module} unwraps them before Thymeleaf sees the
 *       map; nullable references make the {@code != null} guard the canonical template form).
 * </ul>
 *
 * <p>The lab identity block is sourced from {@link LabSettingsApi}.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResultReportModelAssembler {

    private static final DateTimeFormatter FRENCH_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private static final DateTimeFormatter FRENCH_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH)
                    .withZone(ZoneId.of("UTC"));

    private final LabFileApi labFileApi;
    private final ResultApi resultApi;
    private final PatientApi patientApi;
    private final AnalysisApi analysisApi;
    private final AntibioticApi antibioticApi;
    private final UserApi userApi;
    private final LabSettingsApi labSettingsApi;

    @SuppressWarnings(
            "unused") // kept for future "rendered-at" stamping; tests inject a fixed clock
    private final Clock clock;

    public ResultReportModel assemble(LabFileId labFileId) {
        var labFile = labFileApi.requireById(labFileId);

        var results = resultApi.findActiveByLabFile(labFileId);
        if (results.isEmpty()
                || results.stream().anyMatch(r -> r.state() != ResultState.BIOLOGICAL_VALIDATED)) {
            throw new ResultsNotValidatedException(labFileId);
        }

        var patient = patientApi.findById(new PatientId(labFile.patient().id())).orElseThrow();

        // Last (latest) biological validation across all results — defines the report's signature.
        var lastValidated =
                results.stream()
                        .max(Comparator.comparing(ResultSummary::biologicalValidatedAt))
                        .orElseThrow();
        var biologistId = lastValidated.biologicalValidatedBy();
        var biologist = userApi.findById(new UserId(biologistId)).orElseThrow();

        // Fetch analyses to resolve discipline (paillasse) per result.
        var analysisIds = new HashSet<AnalysisId>();
        results.forEach(r -> analysisIds.add(r.analysisId()));
        Map<AnalysisId, AnalysisSummary> analyses = analysisApi.findAllByIds(analysisIds);

        // Index reference intervals by id (one batched fetch per analysis in play), so each line's
        // referenceIntervalId resolves without an N+1. Mirrors the antibiotic batch fetch.
        Map<String, ReferenceIntervalSummary> riById = new HashMap<>();
        for (var analysis : analyses.values()) {
            for (var ri : analysisApi.findActiveReferenceIntervalsByAnalysis(analysis.id())) {
                riById.put(ri.id().value(), ri);
            }
        }

        var groups = buildGroups(results, analyses, riById);
        var antibiograms = buildAntibiograms(results);

        // Electronic-signature attestation (SP3): present only once the dossier is SIGNED.
        String signedByDisplay = null;
        String signedAtDisplay = null;
        if (labFile.signedBy() != null) {
            signedByDisplay =
                    userApi.findById(new UserId(labFile.signedBy()))
                            .map(UserSummary::fullName)
                            .orElse(labFile.signedBy());
            if (labFile.signedAt() != null) {
                signedAtDisplay = FRENCH_DATE_TIME.format(labFile.signedAt());
            }
        }

        var validatedAt = lastValidated.biologicalValidatedAt();
        return new ResultReportModel(
                new ResultReportModel.LabFileSummary(
                        labFile.id().value(),
                        labFile.registrationNumber().value(),
                        labFile.createdAt()),
                buildPatient(patient, labFile.createdAt()),
                buildPractitioner(labFile.prescriber()),
                buildLabIdentity(),
                validatedAt,
                FRENCH_DATE_TIME.format(validatedAt),
                buildUser(biologist),
                signedByDisplay,
                signedAtDisplay,
                groups,
                antibiograms,
                Locale.FRENCH);
    }

    private static ResultReportModel.PatientSummary buildPatient(
            PatientSummary patient, Instant labFileOpenedAt) {
        var firstName = patient.firstNames() == null ? "" : patient.firstNames();
        var dob = patient.birthDate();
        var dobDisplay = dob == null ? "" : dob.format(FRENCH_DATE);
        var age = ageDisplay(dob, labFileOpenedAt);
        return new ResultReportModel.PatientSummary(
                patient.id().value(),
                firstName,
                patient.lastName(),
                patient.sex().name(),
                dobDisplay,
                age);
    }

    private static String ageDisplay(@Nullable LocalDate dob, Instant referenceInstant) {
        if (dob == null) {
            return "";
        }
        var ref = referenceInstant.atZone(ZoneId.of("UTC")).toLocalDate();
        var period = Period.between(dob, ref);
        if (period.getYears() >= 1) {
            return period.getYears() + " ans";
        }
        if (period.getMonths() >= 1) {
            return period.getMonths() + " mois";
        }
        return period.getDays() + " j";
    }

    private static ResultReportModel.@Nullable PractitionerSummary buildPractitioner(
            LabFileSummary.@Nullable PrescriberSnapshot prescriber) {
        if (prescriber == null) {
            return null;
        }
        return new ResultReportModel.PractitionerSummary(
                prescriber.id(),
                prescriber.fullName(),
                prescriber.specialtyCode() == null ? "" : prescriber.specialtyCode());
    }

    private static ResultReportModel.UserSummary buildUser(UserSummary user) {
        return new ResultReportModel.UserSummary(user.id().value(), user.fullName(), "");
    }

    private ResultReportModel.LabIdentity buildLabIdentity() {
        var s = labSettingsApi.get();
        return new ResultReportModel.LabIdentity(
                s.name(), s.address(), s.accreditationNumber(), s.managerName());
    }

    private static List<ResultReportModel.AnalysisGroupBlock> buildGroups(
            List<ResultSummary> results,
            Map<AnalysisId, AnalysisSummary> analyses,
            Map<String, ReferenceIntervalSummary> riById) {
        // Group ResultSummary by paillasse name (discipline name on the analysis snapshot).
        var byPaillasse = new HashMap<String, List<ResultReportModel.AnalysisRow>>();
        for (var result : results) {
            var analysis = analyses.get(result.analysisId());
            var paillasseName = analysis == null ? "Autre" : analysis.discipline().name();
            var rows = byPaillasse.computeIfAbsent(paillasseName, k -> new ArrayList<>());
            int idx = 0;
            for (var line : result.lines()) {
                // Interpretation belongs to the analysis: render it once, under the first row.
                rows.add(toAnalysisRow(line, analysis, riById, idx == 0));
                idx++;
            }
        }
        return byPaillasse.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ResultReportModel.AnalysisGroupBlock(e.getKey(), e.getValue()))
                .toList();
    }

    private static ResultReportModel.AnalysisRow toAnalysisRow(
            ResultSummary.LineSummary line,
            @Nullable AnalysisSummary analysis,
            Map<String, ReferenceIntervalSummary> riById,
            boolean withInterpretation) {
        var ri =
                line.referenceIntervalId() == null
                        ? null
                        : riById.get(line.referenceIntervalId().value());
        BigDecimal numeric = numericValue(line);
        String unit2 = line.unit2Snapshot();
        // Only render a unit-2 value when the analysis actually has a second unit configured, so a
        // misconfigured factor-without-unit can't print a bare number in an empty-unit column.
        String value2 =
                unit2 == null
                        ? null
                        : UnitValueFormatter.unit2Value(
                                numeric, line.conversionFactor(), line.unit2Decimals());
        String interpretation = null;
        if (withInterpretation && analysis != null) {
            var text = analysis.interpretationText();
            if (text != null && !text.isBlank()) {
                interpretation = text;
            }
        }
        return new ResultReportModel.AnalysisRow(
                line.componentCodeSnapshot(),
                line.componentCodeSnapshot(), // label = code by default until catalog labels wired
                displayValue(line),
                line.unit1Snapshot() == null ? "" : line.unit1Snapshot(),
                value2,
                unit2,
                ReferenceIntervalFormatter.unit1(ri),
                unit2 == null ? null : ReferenceIntervalFormatter.unit2(ri),
                line.isOutOfRange(),
                line.commentText(),
                interpretation);
    }

    /** The line's numeric value (INTEGER widened, REAL/FORMULA as-is); {@code null} for text. */
    private static @Nullable BigDecimal numericValue(ResultSummary.LineSummary line) {
        return switch (line.resultType()) {
            case INTEGER ->
                    line.valueInteger() == null ? null : BigDecimal.valueOf(line.valueInteger());
            case REAL, FORMULA -> line.valueReal();
            case TEXT, FREE -> null;
        };
    }

    private static String displayValue(ResultSummary.LineSummary line) {
        return switch (line.resultType()) {
            case INTEGER -> UnitValueFormatter.unit1Value(numericValue(line), line.unit1Decimals());
            // REAL and FORMULA are both numeric in value_real (SP11); format to configured
            // decimals.
            case REAL, FORMULA ->
                    UnitValueFormatter.unit1Value(line.valueReal(), line.unit1Decimals());
            case TEXT, FREE -> line.valueText() == null ? "" : line.valueText();
        };
    }

    private List<ResultReportModel.AntibiogramBlock> buildAntibiograms(
            List<ResultSummary> results) {
        Set<AntibioticId> allAntibioticIds =
                results.stream()
                        .flatMap(r -> r.antibiogramReadings().stream())
                        .map(ResultSummary.AntibiogramReadingSummary::antibioticId)
                        .collect(Collectors.toSet());
        Map<AntibioticId, AntibioticSummary> antibiotics =
                antibioticApi.findAllByIds(allAntibioticIds);

        var blocks = new ArrayList<ResultReportModel.AntibiogramBlock>();
        for (var r : results) {
            if (r.antibiogramReadings().isEmpty()) {
                continue;
            }
            var readings = new ArrayList<ResultReportModel.AntibiogramReading>();
            for (var reading : r.antibiogramReadings()) {
                var abx = antibiotics.get(reading.antibioticId());
                var sigle = abx != null ? abx.code() : reading.antibioticId().value();
                readings.add(
                        new ResultReportModel.AntibiogramReading(
                                sigle,
                                plainDiameter(reading.measuredDiameter()),
                                reading.interpretedSensitivity().name()));
            }
            blocks.add(new ResultReportModel.AntibiogramBlock(r.analysisCodeSnapshot(), readings));
        }
        return blocks;
    }

    private static String plainDiameter(@Nullable BigDecimal diameter) {
        return diameter == null ? "" : diameter.toPlainString();
    }
}
