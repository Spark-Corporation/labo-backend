package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.PreleveurApi;
import org.sparkcorporation.lims.catalog.api.PreleveurId;
import org.sparkcorporation.lims.catalog.api.PreleveurSummary;
import org.sparkcorporation.lims.catalog.api.PrescriberApi;
import org.sparkcorporation.lims.catalog.api.PrescriberId;
import org.sparkcorporation.lims.catalog.api.PrescriberSummary;
import org.sparkcorporation.lims.catalog.api.RecipientApi;
import org.sparkcorporation.lims.catalog.api.RecipientId;
import org.sparkcorporation.lims.catalog.api.RecipientSummary;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileSummary.ExternalReceivedSampleSummary;
import org.sparkcorporation.lims.order.api.LabFileSummary.HomeSampleSummary;
import org.sparkcorporation.lims.order.api.LabFileSummary.InLabSampleSummary;
import org.sparkcorporation.lims.order.domain.ExternalReceivedSample;
import org.sparkcorporation.lims.order.domain.HomeSample;
import org.sparkcorporation.lims.order.domain.InLabSample;
import org.sparkcorporation.lims.order.domain.LabFile;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.NewLabFile;
import org.sparkcorporation.lims.order.domain.Sample;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

class DefaultLabFileApiTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-07T08:00:00Z"), ZoneOffset.UTC);

    private final LabFileRepository fileRepo = mock(LabFileRepository.class);
    private final SampleRepository sampleRepo = mock(SampleRepository.class);
    private final AnalysisRequestRepository requestRepo = mock(AnalysisRequestRepository.class);
    private final PatientApi patientApi = mock(PatientApi.class);
    private final PrescriberApi prescriberApi = mock(PrescriberApi.class);
    private final PreleveurApi preleveurApi = mock(PreleveurApi.class);
    private final AnalysisApi analysisApi = mock(AnalysisApi.class);
    private final RecipientApi recipientApi = mock(RecipientApi.class);

    private final DefaultLabFileApi api =
            new DefaultLabFileApi(
                    fileRepo,
                    sampleRepo,
                    requestRepo,
                    patientApi,
                    prescriberApi,
                    preleveurApi,
                    analysisApi,
                    recipientApi);

    private final String patientId = Ulid.generate();
    private final String prescriberId = Ulid.generate();
    private final String preleveurId = Ulid.generate();
    private final String analysisId = Ulid.generate();
    private final String recipientId = Ulid.generate();

    private LabFile freshFile() {
        var cmd = new NewLabFile(patientId, prescriberId, preleveurId, false, List.of("GLUC"));
        return LabFile.open(cmd, new LabFileRegistrationNumber("LF-260507-0001"), null, FIXED);
    }

    private LabFile freshFileWithoutPrescriber() {
        var cmd = new NewLabFile(patientId, null, null, false, List.of());
        return LabFile.open(cmd, new LabFileRegistrationNumber("LF-260507-0002"), null, FIXED);
    }

    private PatientSummary patientSummary() {
        return new PatientSummary(
                new PatientId(patientId),
                "LAB-260507-001",
                "DOE",
                "Jane",
                Civility.MADAME,
                Sex.F,
                null,
                false,
                false,
                false);
    }

    private PrescriberSummary prescriberSummary() {
        return new PrescriberSummary(
                new PrescriberId(prescriberId),
                "DR-001",
                "Dr",
                "Martin",
                new PrescriberSummary.SpecialtySnapshot(
                        new org.sparkcorporation.lims.catalog.api.SpecialtyId(Ulid.generate()),
                        "CARD",
                        "Cardiology"),
                null,
                null,
                null,
                null);
    }

    private PreleveurSummary preleveurSummary() {
        return new PreleveurSummary(
                new PreleveurId(preleveurId),
                "PRL-001",
                "Préleveur Test",
                null,
                null,
                null,
                Set.of());
    }

    private AnalysisSummary analysisSummary() {
        return new AnalysisSummary(
                new AnalysisId(analysisId),
                "GLUC",
                "Glucose",
                AnalysisKind.SIMPLE,
                new BigDecimal("10.00"),
                1,
                new AnalysisSummary.DisciplineSnapshot(
                        new org.sparkcorporation.lims.catalog.api.DisciplineId(Ulid.generate()),
                        "BIOCHEM",
                        "Biochimie",
                        DisciplinePrefix.BIOCHIMIE),
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

    private RecipientSummary recipientSummary() {
        return new RecipientSummary(
                new RecipientId(recipientId), "TUBE", "Tube", null, null, null, null, true);
    }

    @Test
    void findById_returns_summary_with_all_snapshots_resolved() {
        var file = freshFile();
        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        when(sampleRepo.findByLabFileId(file.getId())).thenReturn(List.of());
        when(requestRepo.findByLabFileId(file.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.of(prescriberSummary()));
        when(preleveurApi.findById(any())).thenReturn(Optional.of(preleveurSummary()));

        var summary = api.findById(file.labFileId()).orElseThrow();

        assertThat(summary.patient().id()).isEqualTo(patientId);
        assertThat(summary.patient().lastName()).isEqualTo("DOE");
        assertThat(summary.patient().sex()).isEqualTo(Sex.F);
        assertThat(summary.prescriber()).isNotNull();
        assertThat(summary.prescriber().code()).isEqualTo("DR-001");
        assertThat(summary.prescriber().specialtyCode()).isEqualTo("CARD");
        assertThat(summary.preleveur()).isNotNull();
        assertThat(summary.preleveur().code()).isEqualTo("PRL-001");
    }

    @Test
    void findById_returns_empty_when_lab_file_missing() {
        when(fileRepo.findActiveById(any())).thenReturn(Optional.empty());
        assertThat(api.findById(LabFileId.next())).isEmpty();
    }

    @Test
    void findById_renders_null_prescriber_when_id_is_null() {
        var file = freshFileWithoutPrescriber();
        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        when(sampleRepo.findByLabFileId(file.getId())).thenReturn(List.of());
        when(requestRepo.findByLabFileId(file.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));

        var summary = api.findById(file.labFileId()).orElseThrow();

        assertThat(summary.prescriber()).isNull();
        assertThat(summary.preleveur()).isNull();
    }

    @Test
    void findById_renders_null_prescriber_when_catalog_returns_empty_soft_deleted() {
        var file = freshFile();
        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        when(sampleRepo.findByLabFileId(file.getId())).thenReturn(List.of());
        when(requestRepo.findByLabFileId(file.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.empty()); // soft-deleted
        when(preleveurApi.findById(any())).thenReturn(Optional.empty()); // soft-deleted

        var summary = api.findById(file.labFileId()).orElseThrow();

        assertThat(summary.prescriber()).isNull();
        assertThat(summary.preleveur()).isNull();
    }

    @Test
    void requireById_throws_LabFileNotFoundException_when_missing() {
        when(fileRepo.findActiveById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.requireById(LabFileId.next()))
                .isInstanceOf(LabFileNotFoundException.class);
    }

    @Test
    void existsById_returns_false_when_active_lookup_empty() {
        when(fileRepo.findActiveById(any())).thenReturn(Optional.empty());
        assertThat(api.existsById(LabFileId.next())).isFalse();
    }

    @Test
    void existsById_returns_true_when_present() {
        var file = freshFile();
        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        assertThat(api.existsById(file.labFileId())).isTrue();
    }

    @Test
    void in_lab_sample_maps_to_InLabSampleSummary() {
        var file = freshFile();
        var sample = InLabSample.create(analysisId, recipientId, FIXED.instant(), Ulid.generate());

        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        when(sampleRepo.findByLabFileId(file.getId())).thenReturn(List.of((Sample) sample));
        when(requestRepo.findByLabFileId(file.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.of(prescriberSummary()));
        when(preleveurApi.findById(any())).thenReturn(Optional.of(preleveurSummary()));
        when(analysisApi.findById(any())).thenReturn(Optional.of(analysisSummary()));
        when(recipientApi.findById(any())).thenReturn(Optional.of(recipientSummary()));

        var summary = api.findById(file.labFileId()).orElseThrow();

        assertThat(summary.samples()).hasSize(1);
        assertThat(summary.samples().get(0)).isInstanceOf(InLabSampleSummary.class);
        var s = (InLabSampleSummary) summary.samples().get(0);
        assertThat(s.analysisCode()).isEqualTo("GLUC");
        assertThat(s.recipientCode()).isEqualTo("TUBE");
    }

    @Test
    void home_sample_maps_to_HomeSampleSummary_with_preleveur_code() {
        var file = freshFile();
        var sample =
                HomeSample.create(
                        analysisId,
                        recipientId,
                        FIXED.instant(),
                        Ulid.generate(),
                        "12 rue X",
                        5,
                        FIXED.instant(),
                        FIXED.instant().plusSeconds(3600),
                        preleveurId);

        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        when(sampleRepo.findByLabFileId(file.getId())).thenReturn(List.of((Sample) sample));
        when(requestRepo.findByLabFileId(file.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.of(prescriberSummary()));
        when(preleveurApi.findById(any())).thenReturn(Optional.of(preleveurSummary()));
        when(analysisApi.findById(any())).thenReturn(Optional.of(analysisSummary()));
        when(recipientApi.findById(any())).thenReturn(Optional.of(recipientSummary()));

        var summary = api.findById(file.labFileId()).orElseThrow();

        assertThat(summary.samples().get(0)).isInstanceOf(HomeSampleSummary.class);
        var s = (HomeSampleSummary) summary.samples().get(0);
        assertThat(s.preleveurCode()).isEqualTo("PRL-001");
        assertThat(s.addressLine()).isEqualTo("12 rue X");
        assertThat(s.distanceKm()).isEqualTo(5);
    }

    @Test
    void external_received_sample_maps_to_ExternalReceivedSampleSummary() {
        var file = freshFile();
        var sample =
                ExternalReceivedSample.create(
                        analysisId,
                        recipientId,
                        null,
                        FIXED.instant(),
                        Ulid.generate(),
                        "Clinique partenaire");

        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        when(sampleRepo.findByLabFileId(file.getId())).thenReturn(List.of((Sample) sample));
        when(requestRepo.findByLabFileId(file.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.of(prescriberSummary()));
        when(preleveurApi.findById(any())).thenReturn(Optional.of(preleveurSummary()));
        when(analysisApi.findById(any())).thenReturn(Optional.of(analysisSummary()));
        when(recipientApi.findById(any())).thenReturn(Optional.of(recipientSummary()));

        var summary = api.findById(file.labFileId()).orElseThrow();

        assertThat(summary.samples().get(0)).isInstanceOf(ExternalReceivedSampleSummary.class);
        var s = (ExternalReceivedSampleSummary) summary.samples().get(0);
        assertThat(s.origin()).isEqualTo("Clinique partenaire");
        assertThat(s.collectedAt()).isNull();
    }

    @Test
    void analysis_code_falls_back_to_question_mark_when_catalog_soft_deleted() {
        var file = freshFile();
        var sample = InLabSample.create(analysisId, recipientId, FIXED.instant(), Ulid.generate());

        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        when(sampleRepo.findByLabFileId(file.getId())).thenReturn(List.of((Sample) sample));
        when(requestRepo.findByLabFileId(file.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.of(prescriberSummary()));
        when(preleveurApi.findById(any())).thenReturn(Optional.of(preleveurSummary()));
        when(analysisApi.findById(any())).thenReturn(Optional.empty());
        when(recipientApi.findById(any())).thenReturn(Optional.empty());

        var summary = api.findById(file.labFileId()).orElseThrow();

        var s = (InLabSampleSummary) summary.samples().get(0);
        assertThat(s.analysisCode()).isEqualTo("?");
        assertThat(s.recipientCode()).isEqualTo("?");
    }

    @Test
    void analysis_request_summary_uses_persisted_analysis_code_snapshot() {
        var file = freshFile();
        // Build a persisted analysis-request via the aggregate path (the package-private
        // attachToFile is not reachable from this package — but addAnalysisRequest wires it).
        file.addAnalysisRequest(
                new org.sparkcorporation.lims.order.domain.NewAnalysisRequest(analysisId, "GLUC"),
                FIXED);
        var persisted = file.analysesView().get(0);

        when(fileRepo.findActiveById(file.getId())).thenReturn(Optional.of(file));
        when(sampleRepo.findByLabFileId(file.getId())).thenReturn(List.of());
        when(requestRepo.findByLabFileId(file.getId())).thenReturn(List.of(persisted));
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.of(prescriberSummary()));
        when(preleveurApi.findById(any())).thenReturn(Optional.of(preleveurSummary()));

        var summary = api.findById(file.labFileId()).orElseThrow();

        assertThat(summary.analyses()).hasSize(1);
        assertThat(summary.analyses().get(0).analysisCode()).isEqualTo("GLUC");
        assertThat(summary.analyses().get(0).analysisId()).isEqualTo(analysisId);
    }

    @Test
    void findActiveByPatient_returns_summaries_for_each_active_file() {
        var f1 = freshFile();
        when(fileRepo.findActiveByPatientId(patientId)).thenReturn(List.of(f1));
        when(sampleRepo.findByLabFileId(f1.getId())).thenReturn(List.of());
        when(requestRepo.findByLabFileId(f1.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.of(prescriberSummary()));
        when(preleveurApi.findById(any())).thenReturn(Optional.of(preleveurSummary()));

        var summaries = api.findActiveByPatient(patientId);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).id().value()).isEqualTo(f1.getId());
    }
}
