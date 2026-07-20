package org.sparkcorporation.lims.order.infra.pdf;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Assembles the {@link WorksheetModel} for a lab file's bench worksheet (CDC §II « feuille de
 * paillasse »). Groups the requested analyses by paillasse (discipline name), maps each sample's
 * daily ID onto its analysis row, and renders the lab identity / patient / footer blocks. Patient
 * identifying data flows through the model; per the logging rules this class logs nothing.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorksheetModelAssembler {

    private static final DateTimeFormatter FRENCH_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH).withZone(ZoneId.of("UTC"));
    private static final DateTimeFormatter FRENCH_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH)
                    .withZone(ZoneId.of("UTC"));

    private final LabFileApi labFileApi;
    private final PatientApi patientApi;
    private final AnalysisApi analysisApi;
    private final LabSettingsApi labSettingsApi;
    private final Clock clock;

    public WorksheetModel assemble(LabFileId labFileId) {
        LabFileSummary labFile = labFileApi.requireById(labFileId);
        var patient = patientApi.requireById(new PatientId(labFile.patient().id()));
        var lab = labSettingsApi.get();

        var analysisIds =
                labFile.analyses().stream().map(a -> new AnalysisId(a.analysisId())).toList();
        Map<AnalysisId, AnalysisSummary> analyses = analysisApi.findAllByIds(analysisIds);

        // analysisCode -> sample dailyId (first matching sample wins)
        var dailyIdByCode = new HashMap<String, String>();
        labFile.samples()
                .forEach(s -> dailyIdByCode.putIfAbsent(s.analysisCode(), s.dailyId().value()));

        var rowsByPaillasse = new HashMap<String, List<WorksheetModel.Row>>();
        for (var request : labFile.analyses()) {
            var analysis = analyses.get(new AnalysisId(request.analysisId()));
            var paillasse = analysis == null ? "Autre" : analysis.discipline().name();
            rowsByPaillasse
                    .computeIfAbsent(paillasse, k -> new ArrayList<>())
                    .add(
                            new WorksheetModel.Row(
                                    dailyIdByCode.getOrDefault(request.analysisCode(), ""),
                                    request.analysisCode(),
                                    analysis == null ? request.analysisCode() : analysis.name(),
                                    analysis == null || analysis.recipient() == null
                                            ? ""
                                            : analysis.recipient().name()));
        }
        var paillasses =
                rowsByPaillasse.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> new WorksheetModel.PaillasseBlock(e.getKey(), e.getValue()))
                        .toList();

        return new WorksheetModel(
                new WorksheetModel.Header(
                        labFile.registrationNumber().value(),
                        FRENCH_DATE.format(labFile.createdAt()),
                        labFile.urgent()),
                new WorksheetModel.PatientBlock(
                        patient.lastName(),
                        patient.firstNames() == null ? "" : patient.firstNames(),
                        ageDisplay(patient.birthDate(), labFile.createdAt()),
                        patient.sex().name()),
                paillasses,
                new WorksheetModel.Footer(
                        FRENCH_DATE_TIME.format(clock.instant()),
                        labFile.preleveur() == null ? null : labFile.preleveur().fullName()),
                new WorksheetModel.LabIdentity(
                        lab.name(), lab.address(), lab.accreditationNumber(), lab.managerName()),
                Locale.FRENCH);
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
