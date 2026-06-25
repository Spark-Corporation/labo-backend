package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.api.SampleDailyId;
import org.sparkcorporation.lims.order.api.SampleId;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.Sex;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LabelServiceTest {

    // Crockford Base32 ULIDs — alphabet EXCLUDES I, L, O, U.
    private static final String LAB_FILE_ULID = "01HZX0000000000000001BF001";
    private static final String PATIENT_ULID = "01HZX00000000000000PAT0001";
    private static final String ANALYSIS_GLY = "01HZX00000000000000ANA0G1Y";
    private static final String ANALYSIS_CRE = "01HZX00000000000000ANA0CRE";
    private static final String ANALYSIS_UREE = "01HZX0000000000000ANA0VREE";
    private static final String ANALYSIS_TGO = "01HZX0000000000000ANA000TG";
    private static final String ANALYSIS_TGP = "01HZX0000000000000ANA00TGP";
    private static final String ANALYSIS_IONO = "01HZX0000000000000ANA00N00";
    private static final String DISCIPLINE_ULID = "01HZX0000000000000000D1SC0";
    private static final String SAMPLE_ULID = "01HZX00000000000000SMP0001";
    private static final String REGISTRATION_NUMBER = "LF-260604-0001";
    private static final Instant CREATED_AT = Instant.parse("2026-06-04T08:00:00Z");

    @Mock private LabFileApi labFileApi;
    @Mock private PatientApi patientApi;
    @Mock private AnalysisApi analysisApi;
    @InjectMocks private LabelService service;

    @Test
    void labelsFor_buildsOneRecordPerSample_withDailyIdCopiesAndFirstFiveAnalyses() {
        stubLabFileWithSixAnalysesAndOneSample();
        stubPatient(LocalDate.of(1985, 7, 12));
        stubAnalyses();

        var response = service.labelsFor(new LabFileId(LAB_FILE_ULID));

        assertThat(response.labFileId()).isEqualTo(LAB_FILE_ULID);
        assertThat(response.labels()).hasSize(1);
        var label = response.labels().get(0);
        assertThat(label.sampleId()).isEqualTo(SAMPLE_ULID);
        assertThat(label.dailyId()).isEqualTo("1040626-001");
        assertThat(label.discipline()).isEqualTo("BIOCHIMIE");
        assertThat(label.copies()).isEqualTo(2);
        assertThat(label.analyses()).containsExactly("GLY", "CREAT", "UREE", "TGO", "TGP");
        assertThat(label.patient().nom()).isEqualTo("DOE");
        assertThat(label.patient().prenom()).isEqualTo("Jane");
        assertThat(label.patient().dateNaissance()).isEqualTo("1985-07-12");
        assertThat(label.patient().age()).isNotBlank();
        assertThat(label.patient().sexe()).isEqualTo("F");
    }

    @Test
    void labelsFor_emptySamples_returnsEmptyLabelsList() {
        stubLabFileWithoutSamples();
        stubPatient(null);

        var response = service.labelsFor(new LabFileId(LAB_FILE_ULID));

        assertThat(response.labels()).isEmpty();
    }

    @Test
    void labelsFor_nullBirthDate_rendersNullDateNaissanceAndEmptyAge() {
        stubLabFileWithSixAnalysesAndOneSample();
        stubPatient(null);
        stubAnalyses();

        var label = service.labelsFor(new LabFileId(LAB_FILE_ULID)).labels().get(0);

        assertThat(label.patient().dateNaissance()).isNull();
        assertThat(label.patient().age()).isEmpty();
    }

    @Test
    void labelsFor_unknownAnalysisForSample_defaultsCopiesToOneAndDisciplineToEmpty() {
        stubLabFileWithSixAnalysesAndOneSample();
        stubPatient(null);
        when(analysisApi.findAllByIds(ArgumentMatchers.anyCollection())).thenReturn(Map.of());

        var label = service.labelsFor(new LabFileId(LAB_FILE_ULID)).labels().get(0);

        assertThat(label.copies()).isEqualTo(1);
        assertThat(label.discipline()).isEmpty();
    }

    @Test
    void labelsFor_unknownLabFile_propagatesNotFound() {
        var id = new LabFileId(LAB_FILE_ULID);
        when(labFileApi.requireById(id)).thenThrow(new LabFileNotFoundException(id));

        assertThatThrownBy(() -> service.labelsFor(id))
                .isInstanceOf(LabFileNotFoundException.class);
    }

    // --- fixture helpers ------------------------------------------------------

    private void stubLabFileWithSixAnalysesAndOneSample() {
        when(labFileApi.requireById(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(labFileSummary(List.of(sample("1040626-001", "GLY"))));
    }

    private void stubLabFileWithoutSamples() {
        when(labFileApi.requireById(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(labFileSummary(List.of()));
    }

    private LabFileSummary labFileSummary(List<LabFileSummary.SampleSummary> samples) {
        return new LabFileSummary(
                new LabFileId(LAB_FILE_ULID),
                new LabFileRegistrationNumber(REGISTRATION_NUMBER),
                new LabFileSummary.PatientSnapshot(PATIENT_ULID, "PAT-0001", "DOE", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                samples,
                List.of(
                        analysisRequest(ANALYSIS_GLY, "GLY"),
                        analysisRequest(ANALYSIS_CRE, "CREAT"),
                        analysisRequest(ANALYSIS_UREE, "UREE"),
                        analysisRequest(ANALYSIS_TGO, "TGO"),
                        analysisRequest(ANALYSIS_TGP, "TGP"),
                        analysisRequest(ANALYSIS_IONO, "IONO")),
                null,
                null,
                new LabFileSummary.BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                CREATED_AT,
                null,
                null,
                null,
                null);
    }

    private LabFileSummary.AnalysisRequestSummary analysisRequest(String analysisId, String code) {
        return new LabFileSummary.AnalysisRequestSummary(
                AnalysisRequestId.next(), analysisId, code, CREATED_AT);
    }

    private LabFileSummary.SampleSummary sample(String dailyId, String analysisCode) {
        return new LabFileSummary.InLabSampleSummary(
                new SampleId(SAMPLE_ULID),
                new SampleDailyId(dailyId),
                analysisCode,
                "TUBE-F",
                CREATED_AT,
                null);
    }

    private void stubPatient(LocalDate birthDate) {
        when(patientApi.requireById(new PatientId(PATIENT_ULID)))
                .thenReturn(
                        new PatientSummary(
                                new PatientId(PATIENT_ULID),
                                "PAT-0001",
                                "DOE",
                                "Jane",
                                Civility.MADAME,
                                Sex.F,
                                birthDate,
                                null,
                                null,
                                false,
                                false));
    }

    private void stubAnalyses() {
        when(analysisApi.findAllByIds(ArgumentMatchers.anyCollection()))
                .thenReturn(Map.of(new AnalysisId(ANALYSIS_GLY), analysisGly()));
    }

    private AnalysisSummary analysisGly() {
        return new AnalysisSummary(
                new AnalysisId(ANALYSIS_GLY),
                "GLY",
                "Glycémie",
                AnalysisKind.SIMPLE,
                BigDecimal.TEN,
                2,
                new AnalysisSummary.DisciplineSnapshot(
                        new org.sparkcorporation.lims.catalog.api.DisciplineId(DISCIPLINE_ULID),
                        "BIOCHIMIE",
                        "Biochimie",
                        DisciplinePrefix.BIOCHIMIE),
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
}
