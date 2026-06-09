package org.sparkcorporation.lims.result.infra.pdf;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.DisciplineApi;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.DisciplineSummary;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.result.infra.DossierCommentRepository;
import org.sparkcorporation.lims.result.infra.PaillasseWorksheetRepository;
import org.sparkcorporation.lims.result.infra.PaillasseWorksheetRowProjection;
import org.sparkcorporation.lims.result.infra.ResultSearchRepository;

/**
 * Builds the {@link PaillasseWorksheetModel} for the CDC §VII per-paillasse worklist — a
 * cross-dossier capture/control sheet for one discipline over a registration-date window. Rows come
 * from the native {@link PaillasseWorksheetRepository}; lab-file / patient / reference-interval
 * snapshots are batch-enriched via the cross-module {@code *Api}s. Stateless, transactional
 * read-only. Patient identifying data flows through the model; per the logging rules this class
 * logs nothing.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaillasseWorksheetModelAssembler {

    private static final DateTimeFormatter FRENCH_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FRENCH_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH)
                    .withZone(ZoneId.of("UTC"));

    private final PaillasseWorksheetRepository repository;
    private final DisciplineApi disciplineApi;
    private final LabFileApi labFileApi;
    private final PatientApi patientApi;
    private final AnalysisApi analysisApi;
    private final LabSettingsApi labSettingsApi;
    private final DossierCommentRepository dossierCommentRepository;
    private final Clock clock;

    public PaillasseWorksheetModel assemble(
            DisciplineId disciplineId, LocalDate from, LocalDate to) {
        // DisciplineNotFoundException lives in catalog.domain (NOT exported through catalog::api
        // per Modulith boundaries), so unknown AND soft-deleted disciplines both surface as a
        // plain EntityNotFoundException -> 404 via the shared advice.
        DisciplineSummary discipline =
                disciplineApi
                        .findById(disciplineId)
                        .filter(DisciplineSummary::active)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "discipline not found: " + disciplineId.value()));
        var lab = labSettingsApi.get();

        var rows =
                repository.findRows(
                        disciplineId.value(),
                        ResultSearchRepository.startOfDay(from),
                        ResultSearchRepository.endOfDay(to));

        // batch-enrich: lab files (registration/samples), patients, reference intervals
        var labFileIds =
                rows.stream()
                        .map(r -> new LabFileId(r.getLabFileId()))
                        .collect(Collectors.toUnmodifiableSet());
        var labFiles = labFileApi.findAllByIds(labFileIds);
        var patientIds =
                rows.stream()
                        .map(r -> new PatientId(r.getPatientId()))
                        .collect(Collectors.toUnmodifiableSet());
        var patients = patientApi.findAllByIds(patientIds);
        var riByAnalysis = new HashMap<AnalysisId, List<ReferenceIntervalSummary>>();

        var blocks = new LinkedHashMap<String, List<PaillasseWorksheetModel.Row>>();
        var meta = new HashMap<String, PaillasseWorksheetRowProjection>();
        for (var row : rows) {
            meta.putIfAbsent(row.getLabFileId(), row);
            blocks.computeIfAbsent(row.getLabFileId(), k -> new ArrayList<>())
                    .add(toRow(row, labFiles, riByAnalysis));
        }

        var dossiers =
                blocks.entrySet().stream()
                        .map(
                                e ->
                                        toDossierBlock(
                                                e.getKey(), e.getValue(), meta, labFiles, patients))
                        .toList();

        return new PaillasseWorksheetModel(
                discipline.name(),
                discipline.code(),
                from.toString(),
                to.toString(),
                "du %s au %s".formatted(FRENCH_DATE.format(from), FRENCH_DATE.format(to)),
                FRENCH_DATE_TIME.format(clock.instant()),
                dossiers,
                new PaillasseWorksheetModel.LabIdentity(
                        lab.name(), lab.address(), lab.accreditationNumber(), lab.managerName()),
                Locale.FRENCH);
    }

    private PaillasseWorksheetModel.Row toRow(
            PaillasseWorksheetRowProjection row,
            Map<LabFileId, LabFileSummary> labFiles,
            Map<AnalysisId, List<ReferenceIntervalSummary>> riByAnalysis) {
        var labFile = labFiles.get(new LabFileId(row.getLabFileId()));
        var dailyId = "";
        if (labFile != null) {
            dailyId =
                    labFile.samples().stream()
                            .filter(s -> s.analysisCode().equals(row.getAnalysisCode()))
                            .map(s -> s.dailyId().value())
                            .findFirst()
                            .orElse("");
        }
        return new PaillasseWorksheetModel.Row(
                dailyId,
                row.getAnalysisCode(),
                row.getComponentCode(),
                displayValue(row),
                row.getUnit1Snapshot() == null ? "" : row.getUnit1Snapshot(),
                referenceDisplay(row, riByAnalysis),
                row.getIsOutOfRange(),
                isTechValidated(row.getResultState()));
    }

    private static String displayValue(PaillasseWorksheetRowProjection row) {
        return switch (row.getResultType()) {
            case "INTEGER" -> row.getValueInteger() == null ? "" : row.getValueInteger().toString();
            case "REAL" -> row.getValueReal() == null ? "" : row.getValueReal().toPlainString();
            default -> row.getValueText() == null ? "" : row.getValueText();
        };
    }

    private static boolean isTechValidated(String resultState) {
        return "TECHNICAL_VALIDATED".equals(resultState)
                || "BIOLOGICAL_VALIDATED".equals(resultState);
    }

    private String referenceDisplay(
            PaillasseWorksheetRowProjection row,
            Map<AnalysisId, List<ReferenceIntervalSummary>> riByAnalysis) {
        if (row.getReferenceIntervalId() == null) {
            return "";
        }
        var analysisId = new AnalysisId(row.getAnalysisId());
        var intervals =
                riByAnalysis.computeIfAbsent(
                        analysisId, analysisApi::findActiveReferenceIntervalsByAnalysis);
        return intervals.stream()
                .filter(ri -> ri.id().value().equals(row.getReferenceIntervalId()))
                .findFirst()
                .map(PaillasseWorksheetModelAssembler::formatInterval)
                .orElse("");
    }

    private static String formatInterval(ReferenceIntervalSummary ri) {
        if (ri.unit1Min() != null && ri.unit1Max() != null) {
            return "%s – %s"
                    .formatted(ri.unit1Min().toPlainString(), ri.unit1Max().toPlainString());
        }
        return ri.commentText() != null ? ri.commentText() : "";
    }

    private PaillasseWorksheetModel.DossierBlock toDossierBlock(
            String labFileId,
            List<PaillasseWorksheetModel.Row> rows,
            Map<String, PaillasseWorksheetRowProjection> meta,
            Map<LabFileId, LabFileSummary> labFiles,
            Map<PatientId, PatientSummary> patients) {
        var projection = meta.get(labFileId);
        var labFile = labFiles.get(new LabFileId(labFileId));
        var registrationNumber =
                labFile != null
                        ? labFile.registrationNumber().value()
                        : projection.getRegistrationNumber();

        var patientId = projection.getPatientId();
        var patient = patients.get(new PatientId(patientId));
        String patientDisplay = patientId;
        String patientType = "";
        if (patient != null) {
            var firstNames = patient.firstNames() == null ? "" : patient.firstNames();
            patientDisplay = (patient.lastName() + " " + firstNames).strip();
            if (patient.insurance() != null) {
                patientType = "Assuré";
            } else if (patient.partner() != null) {
                patientType = "Partenaire";
            }
        }

        var dossierComment =
                dossierCommentRepository
                        .findById(labFileId)
                        .map(c -> c.getCommentText())
                        .orElse(null);

        return new PaillasseWorksheetModel.DossierBlock(
                registrationNumber, patientDisplay, patientType, dossierComment, rows);
    }
}
