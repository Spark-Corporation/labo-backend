package org.sparkcorporation.lims.order.web;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Assembles CDC §IV label records for a lab file — one record per sample. Patient identifying data
 * flows through the response; per the logging rules this class logs nothing.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelService {

    private static final int FIRST_ANALYSES_ON_LABEL = 5;

    private final LabFileApi labFileApi;
    private final PatientApi patientApi;
    private final AnalysisApi analysisApi;

    public LabelResponse labelsFor(LabFileId labFileId) {
        LabFileSummary labFile = labFileApi.requireById(labFileId);
        var patient = patientApi.requireById(new PatientId(labFile.patient().id()));

        var firstAnalyses =
                labFile.analyses().stream()
                        .map(LabFileSummary.AnalysisRequestSummary::analysisCode)
                        .limit(FIRST_ANALYSES_ON_LABEL)
                        .toList();

        // analysisCode -> analysisId from the lab file's own request snapshots,
        // then one batched catalog lookup for labelCount + discipline.
        var idByCode =
                labFile.analyses().stream()
                        .collect(
                                Collectors.toMap(
                                        LabFileSummary.AnalysisRequestSummary::analysisCode,
                                        a -> new AnalysisId(a.analysisId()),
                                        (a, b) -> a));
        var analysisById = analysisApi.findAllByIds(idByCode.values());

        var patientBlock =
                new LabelResponse.PatientBlock(
                        patient.lastName(),
                        patient.firstNames() == null ? "" : patient.firstNames(),
                        patient.birthDate() == null ? null : patient.birthDate().toString(),
                        ageDisplay(patient.birthDate(), labFile.createdAt()),
                        patient.sex().name());

        var labels =
                labFile.samples().stream()
                        .map(
                                sample -> {
                                    var analysis =
                                            Optional.ofNullable(idByCode.get(sample.analysisCode()))
                                                    .map(analysisById::get);
                                    return new LabelResponse.LabelRecord(
                                            sample.id().value(),
                                            sample.dailyId().value(),
                                            analysis.map(a -> a.discipline().code()).orElse(""),
                                            patientBlock,
                                            firstAnalyses,
                                            analysis.map(AnalysisSummary::labelCount).orElse(1));
                                })
                        .toList();

        return new LabelResponse(labFileId.value(), labels);
    }

    private static String ageDisplay(@Nullable LocalDate dob, Instant reference) {
        if (dob == null) {
            return "";
        }
        var ref = reference.atZone(ZoneId.of("UTC")).toLocalDate();
        var period = Period.between(dob, ref);
        if (period.getYears() >= 1) {
            return period.getYears() + " ans";
        }
        if (period.getMonths() >= 1) {
            return period.getMonths() + " mois";
        }
        return period.getDays() + " j";
    }
}
