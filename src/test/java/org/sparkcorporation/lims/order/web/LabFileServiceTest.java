package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import org.sparkcorporation.lims.catalog.api.PreleveurApi;
import org.sparkcorporation.lims.catalog.api.PrescriberApi;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.domain.DuplicateAnalysisRequestException;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.OrderInvalidAnalysisReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPatientReferenceException;
import org.sparkcorporation.lims.order.infra.DefaultLabFileRegistrationGenerator;
import org.sparkcorporation.lims.order.infra.LabFileRepository;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.Sex;

@ExtendWith(MockitoExtension.class)
class LabFileServiceTest {

    private static final String VALID_ULID = "01HZB0000000000000000ADMNZ";
    private static final Instant FIXED_NOW = Instant.parse("2026-05-07T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock private LabFileRepository fileRepo;
    @Mock private LabFileApi labFileApi;
    @Mock private PatientApi patientApi;
    @Mock private PrescriberApi prescriberApi;
    @Mock private PreleveurApi preleveurApi;
    @Mock private AnalysisApi analysisApi;
    @Mock private DefaultLabFileRegistrationGenerator regGen;

    private LabFileService service;

    @BeforeEach
    void setUp() {
        service =
                new LabFileService(
                        fileRepo,
                        labFileApi,
                        patientApi,
                        prescriberApi,
                        preleveurApi,
                        analysisApi,
                        regGen,
                        FIXED_CLOCK);
    }

    private static AnalysisSummary analysisSummary(String code) {
        return new AnalysisSummary(
                AnalysisId.generate(),
                code,
                "Glucose",
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

    private static LabFileSummary fileSummary() {
        return new LabFileSummary(
                new LabFileId(VALID_ULID),
                new LabFileRegistrationNumber("LF-260507-0001"),
                new LabFileSummary.PatientSnapshot(VALID_ULID, "P001", "Doe", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                List.of(),
                List.of(),
                FIXED_NOW,
                null);
    }

    @Test
    void create_validates_patient_first_then_throws_on_unknown_patient() {
        when(patientApi.existsById(any(PatientId.class))).thenReturn(false);

        var req = new CreateLabFileRequest(VALID_ULID, null, null, false, List.of("GLU"));

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(OrderInvalidPatientReferenceException.class);
        // counter is NOT bumped — sequence number must not be burned
        verify(regGen, never()).next(any());
    }

    @Test
    void create_throws_on_unknown_analysis_code_without_burning_counter() {
        when(patientApi.existsById(any(PatientId.class))).thenReturn(true);
        when(analysisApi.findByCode(any(AnalysisCode.class))).thenReturn(Optional.empty());

        var req = new CreateLabFileRequest(VALID_ULID, null, null, false, List.of("UNKNOWN"));

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(OrderInvalidAnalysisReferenceException.class);
        verify(regGen, never()).next(any());
    }

    @Test
    void create_throws_on_duplicate_analysis_in_request() {
        when(patientApi.existsById(any(PatientId.class))).thenReturn(true);
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));

        var req = new CreateLabFileRequest(VALID_ULID, null, null, false, List.of("GLU", "GLU"));

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(DuplicateAnalysisRequestException.class);
        verify(regGen, never()).next(any());
    }

    @Test
    void create_succeeds_and_bumps_counter_after_all_validation_passes() {
        when(patientApi.existsById(any(PatientId.class))).thenReturn(true);
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));
        when(regGen.next(LocalDate.of(2026, 5, 7)))
                .thenReturn(new LabFileRegistrationNumber("LF-260507-0001"));
        when(labFileApi.requireById(any(LabFileId.class))).thenReturn(fileSummary());

        var req = new CreateLabFileRequest(VALID_ULID, null, null, false, List.of("GLU"));

        var resp = service.create(req, "user-1");

        assertThat(resp.registrationNumber()).isEqualTo("LF-260507-0001");
        verify(regGen, times(1)).next(LocalDate.of(2026, 5, 7));
        verify(fileRepo, times(1)).save(any());
    }

    @Test
    void delete_is_idempotent_when_already_inactive() {
        when(fileRepo.findActiveById(VALID_ULID)).thenReturn(Optional.empty());
        service.delete(new LabFileId(VALID_ULID));
        verify(fileRepo, never()).delete(any());
    }

    @Test
    void update_throws_when_file_not_found() {
        when(fileRepo.findActiveById(VALID_ULID)).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                service.update(
                                        new LabFileId(VALID_ULID),
                                        new org.sparkcorporation.lims.order.domain.UpdateLabFile(
                                                true, null, null)))
                .isInstanceOf(LabFileNotFoundException.class);
    }
}
