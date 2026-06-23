package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.domain.DuplicateAnalysisRequestException;
import org.sparkcorporation.lims.order.domain.LabFile;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.NewLabFile;
import org.sparkcorporation.lims.order.domain.OrderInvalidAnalysisReferenceException;
import org.sparkcorporation.lims.order.infra.LabFileRepository;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Unit tests for {@link AnalysisRequestService}. Exercises the four branches missed by JaCoCo —
 * lab-file-missing, duplicate-code-in-request, unknown-analysis-code, and the happy path.
 */
@ExtendWith(MockitoExtension.class)
class AnalysisRequestServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-13T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock private LabFileRepository fileRepo;
    @Mock private AnalysisApi analysisApi;
    @Mock private LabFileApi labFileApi;

    private AnalysisRequestService service;

    @BeforeEach
    void setUp() {
        service = new AnalysisRequestService(fileRepo, analysisApi, labFileApi, FIXED_CLOCK);
    }

    private LabFile freshFile(String patientUlid) {
        return LabFile.open(
                new NewLabFile(
                        patientUlid,
                        null,
                        null,
                        false,
                        List.of(),
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null),
                new LabFileRegistrationNumber("LF-260513-0001"),
                null,
                FIXED_CLOCK);
    }

    private AnalysisSummary analysisSummary(String code) {
        return new AnalysisSummary(
                AnalysisId.generate(),
                code,
                code + " label",
                AnalysisKind.SIMPLE,
                BigDecimal.valueOf(20),
                1,
                new AnalysisSummary.DisciplineSnapshot(
                        DisciplineId.next(), "BIO", "Biochimie", DisciplinePrefix.BIOCHIMIE),
                null,
                null,
                ResultType.REAL,
                null,
                null,
                null,
                new AnalysisSummary.UnitsSummary(null, null, null, null, null),
                new AnalysisSummary.AnalyticalDelaySummary(null, null, null),
                List.of(),
                List.of());
    }

    private LabFileSummary summaryFor(LabFile file) {
        return new LabFileSummary(
                file.labFileId(),
                file.getRegistrationNumber(),
                new LabFileSummary.PatientSnapshot(
                        file.getPatientId(), "P001", "Doe", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                List.of(),
                List.of(),
                null,
                null,
                new LabFileSummary.BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                FIXED_NOW,
                null,
                null,
                null,
                null);
    }

    @Test
    void addAnalyses_throws_LabFileNotFoundException_when_file_missing() {
        var fileId = LabFileId.next();
        when(fileRepo.findActiveById(fileId.value())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addAnalyses(fileId, List.of("GLU")))
                .isInstanceOf(LabFileNotFoundException.class);

        verify(analysisApi, never()).findByCode(any(AnalysisCode.class));
    }

    @Test
    void addAnalyses_throws_on_duplicate_code_in_request() {
        var file = freshFile(Ulid.generate());
        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));

        assertThatThrownBy(
                        () ->
                                service.addAnalyses(
                                        new LabFileId(file.getId()), List.of("GLU", "GLU")))
                .isInstanceOf(DuplicateAnalysisRequestException.class);
    }

    @Test
    void addAnalyses_throws_on_unknown_code() {
        var file = freshFile(Ulid.generate());
        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        when(analysisApi.findByCode(any(AnalysisCode.class))).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> service.addAnalyses(new LabFileId(file.getId()), List.of("UNKNOWN")))
                .isInstanceOf(OrderInvalidAnalysisReferenceException.class);
    }

    @Test
    void addAnalyses_happy_path_returns_updated_response() {
        var file = freshFile(Ulid.generate());
        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));
        when(labFileApi.requireById(new LabFileId(file.getId()))).thenReturn(summaryFor(file));

        var resp = service.addAnalyses(new LabFileId(file.getId()), List.of("GLU"));

        // Should have routed through labFileApi for the response hydration.
        verify(labFileApi).requireById(new LabFileId(file.getId()));
        org.assertj.core.api.Assertions.assertThat(resp.registrationNumber())
                .isEqualTo("LF-260513-0001");
    }

    @Test
    void removeAnalysis_throws_when_file_missing() {
        var fileId = LabFileId.next();
        when(fileRepo.findActiveById(fileId.value())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeAnalysis(fileId, AnalysisRequestId.next()))
                .isInstanceOf(LabFileNotFoundException.class);
    }
}
