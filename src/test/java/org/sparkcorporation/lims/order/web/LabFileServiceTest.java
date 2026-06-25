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
import org.springframework.context.ApplicationEventPublisher;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.InsuranceApi;
import org.sparkcorporation.lims.catalog.api.InsuranceId;
import org.sparkcorporation.lims.catalog.api.PartnerApi;
import org.sparkcorporation.lims.catalog.api.PartnerId;
import org.sparkcorporation.lims.catalog.api.PartnerKind;
import org.sparkcorporation.lims.catalog.api.PreleveurApi;
import org.sparkcorporation.lims.catalog.api.PrescriberApi;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestAdded;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.domain.DuplicateAnalysisRequestException;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.OrderInvalidAnalysisReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidInsuranceReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPartnerReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPatientReferenceException;
import org.sparkcorporation.lims.order.infra.DefaultLabFileRegistrationGenerator;
import org.sparkcorporation.lims.order.infra.LabFileRepository;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

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
    @Mock private InsuranceApi insuranceApi;
    @Mock private PartnerApi partnerApi;
    @Mock private AnalysisApi analysisApi;
    @Mock private DefaultLabFileRegistrationGenerator regGen;
    @Mock private ApplicationEventPublisher events;

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
                        insuranceApi,
                        partnerApi,
                        analysisApi,
                        regGen,
                        events,
                        FIXED_CLOCK);
    }

    private static PatientSummary patientSummary() {
        return new PatientSummary(
                new org.sparkcorporation.lims.patient.api.PatientId(VALID_ULID),
                "P001",
                "Doe",
                "Jane",
                Civility.MADAME,
                Sex.F,
                null,
                null,
                null,
                false,
                false);
    }

    private static PatientSummary patientSummaryWithInsurance(String insuranceUlid) {
        return new PatientSummary(
                new org.sparkcorporation.lims.patient.api.PatientId(VALID_ULID),
                "P001",
                "Doe",
                "Jane",
                Civility.MADAME,
                Sex.F,
                null,
                new PatientSummary.InsuranceSnapshot(
                        new InsuranceId(insuranceUlid), "CNAM", "Caisse"),
                null,
                false,
                false);
    }

    private static PatientSummary patientSummaryWithPartner(String partnerUlid) {
        return new PatientSummary(
                new org.sparkcorporation.lims.patient.api.PatientId(VALID_ULID),
                "P001",
                "Doe",
                "Jane",
                Civility.MADAME,
                Sex.F,
                null,
                null,
                new PatientSummary.PartnerSnapshot(
                        new PartnerId(partnerUlid), "SOC-1", "Société", PartnerKind.SOCIETY),
                false,
                false);
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
    void create_validates_patient_first_then_throws_on_unknown_patient() {
        when(patientApi.findById(any(PatientId.class))).thenReturn(Optional.empty());

        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("GLU"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(OrderInvalidPatientReferenceException.class);
        // counter is NOT bumped — sequence number must not be burned
        verify(regGen, never()).next(any());
    }

    @Test
    void create_throws_on_unknown_analysis_code_without_burning_counter() {
        when(patientApi.findById(any(PatientId.class))).thenReturn(Optional.of(patientSummary()));
        when(analysisApi.findByCode(any(AnalysisCode.class))).thenReturn(Optional.empty());

        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("UNKNOWN"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(OrderInvalidAnalysisReferenceException.class);
        verify(regGen, never()).next(any());
    }

    @Test
    void create_throws_on_duplicate_analysis_in_request() {
        when(patientApi.findById(any(PatientId.class))).thenReturn(Optional.of(patientSummary()));
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));

        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("GLU", "GLU"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(DuplicateAnalysisRequestException.class);
        verify(regGen, never()).next(any());
    }

    @Test
    void create_succeeds_and_bumps_counter_after_all_validation_passes() {
        when(patientApi.findById(any(PatientId.class))).thenReturn(Optional.of(patientSummary()));
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));
        when(regGen.next(LocalDate.of(2026, 5, 7)))
                .thenReturn(new LabFileRegistrationNumber("LF-260507-0001"));
        when(labFileApi.requireById(any(LabFileId.class))).thenReturn(fileSummary());

        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("GLU"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        var resp = service.create(req, "user-1");

        assertThat(resp.registrationNumber()).isEqualTo("LF-260507-0001");
        verify(regGen, times(1)).next(LocalDate.of(2026, 5, 7));
        verify(fileRepo, times(1)).save(any());
        verify(events, times(1)).publishEvent(any(AnalysisRequestAdded.class));
    }

    @Test
    void create_throws_on_unknown_insurance_reference() {
        var insuranceUlid = Ulid.generate();
        when(patientApi.findById(any(PatientId.class))).thenReturn(Optional.of(patientSummary()));
        when(insuranceApi.exists(any(InsuranceId.class))).thenReturn(false);

        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("GLU"),
                        insuranceUlid,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(OrderInvalidInsuranceReferenceException.class);
        verify(regGen, never()).next(any());
    }

    @Test
    void create_throws_on_unknown_partner_reference() {
        var partnerUlid = Ulid.generate();
        when(patientApi.findById(any(PatientId.class))).thenReturn(Optional.of(patientSummary()));
        when(partnerApi.exists(any(PartnerId.class))).thenReturn(false);

        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("GLU"),
                        null,
                        partnerUlid,
                        null,
                        null,
                        null,
                        null,
                        null);

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(OrderInvalidPartnerReferenceException.class);
        verify(regGen, never()).next(any());
    }

    @Test
    void create_defaults_insurance_from_patient_when_request_omits_it() {
        var insuranceUlid = Ulid.generate();
        when(patientApi.findById(any(PatientId.class)))
                .thenReturn(Optional.of(patientSummaryWithInsurance(insuranceUlid)));
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));
        when(regGen.next(LocalDate.of(2026, 5, 7)))
                .thenReturn(new LabFileRegistrationNumber("LF-260507-0001"));
        when(labFileApi.requireById(any(LabFileId.class))).thenReturn(fileSummary());

        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("GLU"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        service.create(req, "user-1");

        // patient's insurance is used as default — no validation call needed (FK in DB owned by
        // patient's create-time validation).
        verify(insuranceApi, never()).exists(any());
        verify(fileRepo, times(1))
                .save(
                        org.mockito.ArgumentMatchers.argThat(
                                lf -> insuranceUlid.equals(lf.getInsuranceId())));
    }

    @Test
    void create_defaults_partner_from_patient_when_request_omits_it() {
        var partnerUlid = Ulid.generate();
        when(patientApi.findById(any(PatientId.class)))
                .thenReturn(Optional.of(patientSummaryWithPartner(partnerUlid)));
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));
        when(regGen.next(LocalDate.of(2026, 5, 7)))
                .thenReturn(new LabFileRegistrationNumber("LF-260507-0001"));
        when(labFileApi.requireById(any(LabFileId.class))).thenReturn(fileSummary());

        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("GLU"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        service.create(req, "user-1");

        verify(partnerApi, never()).exists(any());
        verify(fileRepo, times(1))
                .save(
                        org.mockito.ArgumentMatchers.argThat(
                                lf -> partnerUlid.equals(lf.getPartnerId())));
    }

    @Test
    void create_explicit_insurance_overrides_patient_default() {
        var patientInsuranceUlid = Ulid.generate();
        var explicitInsuranceUlid = Ulid.generate();
        when(patientApi.findById(any(PatientId.class)))
                .thenReturn(Optional.of(patientSummaryWithInsurance(patientInsuranceUlid)));
        when(insuranceApi.exists(new InsuranceId(explicitInsuranceUlid))).thenReturn(true);
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));
        when(regGen.next(LocalDate.of(2026, 5, 7)))
                .thenReturn(new LabFileRegistrationNumber("LF-260507-0001"));
        when(labFileApi.requireById(any(LabFileId.class))).thenReturn(fileSummary());

        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("GLU"),
                        explicitInsuranceUlid,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        service.create(req, "user-1");

        verify(fileRepo, times(1))
                .save(
                        org.mockito.ArgumentMatchers.argThat(
                                lf -> explicitInsuranceUlid.equals(lf.getInsuranceId())));
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
                                                true, null, null, null, null, null, null, null,
                                                null)))
                .isInstanceOf(LabFileNotFoundException.class);
    }

    @Test
    void create_throws_on_unknown_prescriber_reference() {
        var prescriberUlid = Ulid.generate();
        when(patientApi.findById(any(PatientId.class))).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.existsById(any())).thenReturn(false);

        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        prescriberUlid,
                        null,
                        false,
                        List.of("GLU"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(
                        org.sparkcorporation.lims.order.domain
                                .OrderInvalidPrescriberReferenceException.class);
        verify(regGen, never()).next(any());
    }

    @Test
    void create_throws_on_unknown_preleveur_reference() {
        var preleveurUlid = Ulid.generate();
        when(patientApi.findById(any(PatientId.class))).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.existsById(any())).thenReturn(true);
        when(preleveurApi.existsById(any())).thenReturn(false);

        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        Ulid.generate(),
                        preleveurUlid,
                        false,
                        List.of("GLU"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(
                        org.sparkcorporation.lims.order.domain
                                .OrderInvalidPreleveurReferenceException.class);
        verify(regGen, never()).next(any());
    }

    @Test
    void update_throws_on_unknown_insurance_reference() {
        var insuranceUlid = Ulid.generate();
        when(insuranceApi.exists(new InsuranceId(insuranceUlid))).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.update(
                                        new LabFileId(VALID_ULID),
                                        new org.sparkcorporation.lims.order.domain.UpdateLabFile(
                                                null,
                                                null,
                                                null,
                                                insuranceUlid,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null)))
                .isInstanceOf(OrderInvalidInsuranceReferenceException.class);
        verify(fileRepo, never()).findActiveById(any());
    }

    @Test
    void update_throws_on_unknown_partner_reference() {
        var partnerUlid = Ulid.generate();
        when(partnerApi.exists(new PartnerId(partnerUlid))).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.update(
                                        new LabFileId(VALID_ULID),
                                        new org.sparkcorporation.lims.order.domain.UpdateLabFile(
                                                null,
                                                null,
                                                null,
                                                null,
                                                partnerUlid,
                                                null,
                                                null,
                                                null,
                                                null)))
                .isInstanceOf(OrderInvalidPartnerReferenceException.class);
        verify(fileRepo, never()).findActiveById(any());
    }

    @Test
    void delete_when_present_calls_repository_delete() {
        var patient = patientSummary();
        var file =
                org.sparkcorporation.lims.order.domain.LabFile.open(
                        new org.sparkcorporation.lims.order.domain.NewLabFile(
                                patient.id().value(),
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
                        new LabFileRegistrationNumber("LF-260507-0001"),
                        null,
                        FIXED_CLOCK);
        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));

        service.delete(new LabFileId(file.getId()));

        verify(fileRepo, times(1)).delete(file);
    }
}
