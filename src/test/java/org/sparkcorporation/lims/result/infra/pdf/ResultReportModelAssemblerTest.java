package org.sparkcorporation.lims.result.infra.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.AntibioticApi;
import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.AntibioticSummary;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.iam.api.UserApi;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.iam.api.UserSummary;
import org.sparkcorporation.lims.labsettings.api.LabSettings;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.result.api.AntibiogramReadingId;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.api.ResultSummary;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sensitivity;
import org.sparkcorporation.lims.shared.domain.Sex;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResultReportModelAssemblerTest {

    // Valid 26-char Crockford Base32 ULIDs (alphabet: 0-9 A-H J K M N P Q R S T V-Z; no I, L, O,
    // U).
    private static final String LAB_FILE_ULID = "01HZB0000000000000000KBFE0";
    private static final String PATIENT_ULID = "01HZB0000000000000000PATE0";
    private static final String RESULT_ULID_1 = "01HZB000000000000000RST001";
    private static final String RESULT_ULID_2 = "01HZB000000000000000RST002";
    private static final String AR_ULID_1 = "01HZB000000000000000ANRQ01";
    private static final String AR_ULID_2 = "01HZB000000000000000ANRQ02";
    private static final String ANALYSIS_ULID_1 = "01HZB000000000000000ANYS01";
    private static final String ANALYSIS_ULID_2 = "01HZB000000000000000ANYS02";
    private static final String LINE_ULID_1 = "01HZB000000000000000RNE001";
    private static final String LINE_ULID_2 = "01HZB000000000000000RNE002";
    private static final String DISCIPLINE_BIOCH = "01HZB000000000000000DSCB00";
    private static final String DISCIPLINE_HEMA = "01HZB000000000000000DSCH00";
    private static final String BIOLOGIST_ULID = "01HZB000000000000000BGST00";
    private static final String ANTIBIOTIC_ULID = "01HZB000000000000000ATBC01";
    private static final String READING_ULID = "01HZB000000000000000RDNG01";

    private static final Instant VALIDATED_AT = Instant.parse("2026-05-20T12:30:00Z");
    private static final Instant OPENED_AT = Instant.parse("2026-05-20T08:00:00Z");
    private static final Instant SIGNED_AT = Instant.parse("2026-05-28T14:10:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(VALIDATED_AT, ZoneId.of("UTC"));

    @Mock private LabFileApi labFileApi;
    @Mock private ResultApi resultApi;
    @Mock private PatientApi patientApi;
    @Mock private AnalysisApi analysisApi;
    @Mock private AntibioticApi antibioticApi;
    @Mock private UserApi userApi;
    @Mock private LabSettingsApi labSettingsApi;

    @InjectMocks private ResultReportModelAssembler assembler;

    @Test
    void assembleProducesExpectedModel() {
        // Two biologically-validated results, one per discipline.
        var labFileId = new LabFileId(LAB_FILE_ULID);
        var labFile = labFileWith(LabFileStatus.OPEN);
        when(labFileApi.requireById(labFileId)).thenReturn(labFile);

        when(labSettingsApi.get())
                .thenReturn(
                        new LabSettings(
                                "Test Lab",
                                "1 rue du Test, Test City",
                                "ACC-TEST-001",
                                "Dr. Test",
                                Instant.parse("2026-05-21T10:00:00Z")));

        var line1 =
                new ResultSummary.LineSummary(
                        new ResultLineId(LINE_ULID_1),
                        new AnalysisId(ANALYSIS_ULID_1),
                        "GLU",
                        0,
                        ResultType.REAL,
                        null,
                        new BigDecimal("1.05"),
                        null,
                        "g/L",
                        2,
                        null,
                        null,
                        null,
                        false,
                        null);
        var line2 =
                new ResultSummary.LineSummary(
                        new ResultLineId(LINE_ULID_2),
                        new AnalysisId(ANALYSIS_ULID_2),
                        "HB",
                        0,
                        ResultType.REAL,
                        null,
                        new BigDecimal("9.20"),
                        null,
                        "g/dL",
                        2,
                        null,
                        null,
                        null,
                        true,
                        "Patient à recontrôler");

        var reading =
                new ResultSummary.AntibiogramReadingSummary(
                        new AntibiogramReadingId(READING_ULID),
                        new AntibioticId(ANTIBIOTIC_ULID),
                        new BigDecimal("18.0"),
                        Sensitivity.S,
                        0);

        var r1 =
                new ResultSummary(
                        new ResultId(RESULT_ULID_1),
                        labFileId,
                        new AnalysisRequestId(AR_ULID_1),
                        new AnalysisId(ANALYSIS_ULID_1),
                        "GLU",
                        ResultState.BIOLOGICAL_VALIDATED,
                        "tech",
                        Instant.parse("2026-05-20T11:00:00Z"),
                        BIOLOGIST_ULID,
                        VALIDATED_AT,
                        List.of(line1),
                        List.of(),
                        OPENED_AT,
                        null);
        var r2 =
                new ResultSummary(
                        new ResultId(RESULT_ULID_2),
                        labFileId,
                        new AnalysisRequestId(AR_ULID_2),
                        new AnalysisId(ANALYSIS_ULID_2),
                        "HB",
                        ResultState.BIOLOGICAL_VALIDATED,
                        "tech",
                        Instant.parse("2026-05-20T11:30:00Z"),
                        BIOLOGIST_ULID,
                        VALIDATED_AT,
                        List.of(line2),
                        List.of(reading),
                        OPENED_AT,
                        null);

        when(resultApi.findActiveByLabFile(labFileId)).thenReturn(List.of(r1, r2));

        when(patientApi.findById(new PatientId(PATIENT_ULID)))
                .thenReturn(Optional.of(patientSummary()));

        when(analysisApi.findAllByIds(anyCollection()))
                .thenReturn(
                        Map.of(
                                new AnalysisId(ANALYSIS_ULID_1),
                                analysisSummary(
                                        ANALYSIS_ULID_1,
                                        "GLU",
                                        "Glucose",
                                        DISCIPLINE_BIOCH,
                                        "Biochimie"),
                                new AnalysisId(ANALYSIS_ULID_2),
                                analysisSummary(
                                        ANALYSIS_ULID_2,
                                        "HB",
                                        "Hémoglobine",
                                        DISCIPLINE_HEMA,
                                        "Hématologie")));

        var amxId = new AntibioticId(ANTIBIOTIC_ULID);
        var amxSummary =
                new AntibioticSummary(
                        amxId,
                        "AMX",
                        "Amoxicilline",
                        null,
                        "Pénicillines",
                        new BigDecimal("18.0"),
                        null,
                        null,
                        true);
        when(antibioticApi.findAllByIds(Set.of(amxId))).thenReturn(Map.of(amxId, amxSummary));

        when(userApi.findById(new UserId(BIOLOGIST_ULID)))
                .thenReturn(
                        Optional.of(
                                new UserSummary(
                                        new UserId(BIOLOGIST_ULID),
                                        "biologist@test.local",
                                        "Dr. KOFFI Adjoa",
                                        true,
                                        Set.of())));

        // When.
        var model = assembler.assemble(labFileId);

        // Then — lab file block.
        assertThat(model.labFile().labFileId()).isEqualTo(LAB_FILE_ULID);
        assertThat(model.labFile().dailyId()).isEqualTo("LF-260520-9001");
        assertThat(model.labFile().openedAt()).isEqualTo(OPENED_AT);

        // Patient block.
        assertThat(model.patient().patientId()).isEqualTo(PATIENT_ULID);
        assertThat(model.patient().lastName()).isEqualTo("DOE");
        assertThat(model.patient().firstName()).isEqualTo("Jane");
        assertThat(model.patient().sex()).isEqualTo("F");
        assertThat(model.patient().dateOfBirth()).isEqualTo("01/01/1985");

        // No prescriber on labFile snapshot → no practitioner on model.
        assertThat(model.requestingPractitioner()).isNull();

        // Lab identity is sourced from LabSettingsApi.
        assertThat(model.labIdentity().name()).isEqualTo("Test Lab");
        assertThat(model.labIdentity().address()).isEqualTo("1 rue du Test, Test City");
        assertThat(model.labIdentity().accreditationNumber()).isEqualTo("ACC-TEST-001");
        assertThat(model.labIdentity().managerName()).isEqualTo("Dr. Test");

        // Biological-validation block.
        assertThat(model.biologicalValidatedAt()).isEqualTo(VALIDATED_AT);
        assertThat(model.biologicalValidatedAtDisplay()).isEqualTo("20/05/2026 12:30");
        assertThat(model.biologicalValidatedBy().userId()).isEqualTo(BIOLOGIST_ULID);
        assertThat(model.biologicalValidatedBy().displayName()).isEqualTo("Dr. KOFFI Adjoa");

        // Unsigned dossier → no electronic-signature attestation.
        assertThat(model.signedByDisplay()).isNull();
        assertThat(model.signedAtDisplay()).isNull();

        // Groups — one per discipline, ordered alphabetically.
        assertThat(model.groups()).hasSize(2);
        var biochimieGroup =
                model.groups().stream()
                        .filter(g -> "Biochimie".equals(g.paillasseName()))
                        .findFirst()
                        .orElseThrow();
        assertThat(biochimieGroup.rows()).hasSize(1);
        assertThat(biochimieGroup.rows().get(0).code()).isEqualTo("GLU");
        assertThat(biochimieGroup.rows().get(0).valueDisplay()).isEqualTo("1.05");
        assertThat(biochimieGroup.rows().get(0).unit()).isEqualTo("g/L");
        assertThat(biochimieGroup.rows().get(0).outOfRange()).isFalse();

        var hemaGroup =
                model.groups().stream()
                        .filter(g -> "Hématologie".equals(g.paillasseName()))
                        .findFirst()
                        .orElseThrow();
        assertThat(hemaGroup.rows()).hasSize(1);
        assertThat(hemaGroup.rows().get(0).code()).isEqualTo("HB");
        assertThat(hemaGroup.rows().get(0).outOfRange()).isTrue();
        assertThat(hemaGroup.rows().get(0).comment()).isEqualTo("Patient à recontrôler");

        // Antibiograms — one block for the result with a reading.
        assertThat(model.antibiograms()).hasSize(1);
        assertThat(model.antibiograms().get(0).readings()).hasSize(1);
        assertThat(model.antibiograms().get(0).readings().get(0).antibioticSigle())
                .isEqualTo("AMX");
        assertThat(model.antibiograms().get(0).readings().get(0).interpretation()).isEqualTo("S");

        // Locale.
        assertThat(model.locale()).isNotNull();
    }

    @Test
    void assembleSignedDossierCarriesElectronicSignatureAttestation() {
        var labFileId = new LabFileId(LAB_FILE_ULID);
        when(labFileApi.requireById(labFileId))
                .thenReturn(signedLabFileWith(BIOLOGIST_ULID, SIGNED_AT));

        when(labSettingsApi.get())
                .thenReturn(
                        new LabSettings(
                                "Test Lab",
                                "1 rue du Test, Test City",
                                "ACC-TEST-001",
                                "Dr. Test",
                                Instant.parse("2026-05-21T10:00:00Z")));

        var line =
                new ResultSummary.LineSummary(
                        new ResultLineId(LINE_ULID_1),
                        new AnalysisId(ANALYSIS_ULID_1),
                        "GLU",
                        0,
                        ResultType.REAL,
                        null,
                        new BigDecimal("1.05"),
                        null,
                        "g/L",
                        2,
                        null,
                        null,
                        null,
                        false,
                        null);
        var r1 =
                new ResultSummary(
                        new ResultId(RESULT_ULID_1),
                        labFileId,
                        new AnalysisRequestId(AR_ULID_1),
                        new AnalysisId(ANALYSIS_ULID_1),
                        "GLU",
                        ResultState.BIOLOGICAL_VALIDATED,
                        "tech",
                        Instant.parse("2026-05-20T11:00:00Z"),
                        BIOLOGIST_ULID,
                        VALIDATED_AT,
                        List.of(line),
                        List.of(),
                        OPENED_AT,
                        null);
        when(resultApi.findActiveByLabFile(labFileId)).thenReturn(List.of(r1));

        when(patientApi.findById(new PatientId(PATIENT_ULID)))
                .thenReturn(Optional.of(patientSummary()));
        when(analysisApi.findAllByIds(anyCollection()))
                .thenReturn(
                        Map.of(
                                new AnalysisId(ANALYSIS_ULID_1),
                                analysisSummary(
                                        ANALYSIS_ULID_1,
                                        "GLU",
                                        "Glucose",
                                        DISCIPLINE_BIOCH,
                                        "Biochimie")));
        when(userApi.findById(new UserId(BIOLOGIST_ULID)))
                .thenReturn(
                        Optional.of(
                                new UserSummary(
                                        new UserId(BIOLOGIST_ULID),
                                        "biologist@test.local",
                                        "Dr. KOFFI Adjoa",
                                        true,
                                        Set.of())));

        var model = assembler.assemble(labFileId);

        assertThat(model.signedByDisplay()).isEqualTo("Dr. KOFFI Adjoa");
        assertThat(model.signedAtDisplay()).isEqualTo("28/05/2026 14:10");
    }

    @Test
    void missingLabFileThrows() {
        var labFileId = new LabFileId(LAB_FILE_ULID);
        when(labFileApi.requireById(labFileId))
                .thenThrow(
                        new org.sparkcorporation.lims.order.domain.LabFileNotFoundException(
                                labFileId));

        assertThatExceptionOfType(
                        org.sparkcorporation.lims.order.domain.LabFileNotFoundException.class)
                .isThrownBy(() -> assembler.assemble(labFileId));
    }

    @Test
    void noBiologicalValidationThrows() {
        var labFileId = new LabFileId(LAB_FILE_ULID);
        when(labFileApi.requireById(labFileId)).thenReturn(labFileWith(LabFileStatus.OPEN));

        // One result still in TECHNICAL_VALIDATED, not BIOLOGICAL_VALIDATED.
        var line =
                new ResultSummary.LineSummary(
                        new ResultLineId(LINE_ULID_1),
                        new AnalysisId(ANALYSIS_ULID_1),
                        "GLU",
                        0,
                        ResultType.REAL,
                        null,
                        new BigDecimal("1.05"),
                        null,
                        "g/L",
                        2,
                        null,
                        null,
                        null,
                        false,
                        null);
        var pending =
                new ResultSummary(
                        new ResultId(RESULT_ULID_1),
                        labFileId,
                        new AnalysisRequestId(AR_ULID_1),
                        new AnalysisId(ANALYSIS_ULID_1),
                        "GLU",
                        ResultState.TECHNICAL_VALIDATED,
                        "tech",
                        Instant.parse("2026-05-20T11:00:00Z"),
                        null,
                        null,
                        List.of(line),
                        List.of(),
                        OPENED_AT,
                        null);
        when(resultApi.findActiveByLabFile(labFileId)).thenReturn(List.of(pending));

        assertThatExceptionOfType(ResultsNotValidatedException.class)
                .isThrownBy(() -> assembler.assemble(labFileId))
                .matches(ex -> LAB_FILE_ULID.equals(ex.labFileId()));
    }

    private static LabFileSummary labFileWith(LabFileStatus status) {
        return new LabFileSummary(
                new LabFileId(LAB_FILE_ULID),
                new LabFileRegistrationNumber("LF-260520-9001"),
                new LabFileSummary.PatientSnapshot(PATIENT_ULID, "LAB-00001", "DOE", "Jane", Sex.F),
                null,
                null,
                false,
                status,
                List.of(),
                List.of(),
                null,
                null,
                new LabFileSummary.BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                OPENED_AT,
                null,
                null,
                null,
                null);
    }

    private static LabFileSummary signedLabFileWith(String signedBy, Instant signedAt) {
        return new LabFileSummary(
                new LabFileId(LAB_FILE_ULID),
                new LabFileRegistrationNumber("LF-260520-9001"),
                new LabFileSummary.PatientSnapshot(PATIENT_ULID, "LAB-00001", "DOE", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.SIGNED,
                List.of(),
                List.of(),
                null,
                null,
                new LabFileSummary.BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                OPENED_AT,
                null,
                signedBy,
                signedAt,
                null);
    }

    private static PatientSummary patientSummary() {
        return new PatientSummary(
                new PatientId(PATIENT_ULID),
                "LAB-00001",
                "DOE",
                "Jane",
                Civility.MADAME,
                Sex.F,
                LocalDate.of(1985, 1, 1),
                null,
                null,
                false,
                false);
    }

    private static AnalysisSummary analysisSummary(
            String id, String code, String name, String disciplineId, String disciplineName) {
        return new AnalysisSummary(
                new AnalysisId(id),
                code,
                name,
                AnalysisKind.SIMPLE,
                new BigDecimal("10.00"),
                1,
                new AnalysisSummary.DisciplineSnapshot(
                        new DisciplineId(disciplineId),
                        disciplineName.substring(0, 4).toUpperCase(),
                        disciplineName,
                        org.sparkcorporation.lims.shared.domain.DisciplinePrefix.BIOCHIMIE),
                null,
                null,
                ResultType.REAL,
                null,
                null,
                null,
                null,
                new AnalysisSummary.UnitsSummary("g/L", 2, null, null, null),
                new AnalysisSummary.AnalyticalDelaySummary(1, null, null),
                List.of(),
                List.of());
    }
}
