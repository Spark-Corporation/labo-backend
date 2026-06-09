package org.sparkcorporation.lims.result.infra.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.DisciplineApi;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.DisciplineSummary;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;
import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.labsettings.api.LabSettings;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.api.SampleDailyId;
import org.sparkcorporation.lims.order.api.SampleId;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.result.domain.DossierComment;
import org.sparkcorporation.lims.result.infra.DossierCommentRepository;
import org.sparkcorporation.lims.result.infra.PaillasseWorksheetRepository;
import org.sparkcorporation.lims.result.infra.PaillasseWorksheetRowProjection;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaillasseWorksheetModelAssemblerTest {

    // Valid 26-char Crockford Base32 ULIDs (alphabet excludes I, L, O, U).
    private static final String DISC_BIO_ULID = "01HZX0000000000000DSCB10C1";
    private static final String LAB_FILE_1 = "01HZX0000000000000001BF001";
    private static final String LAB_FILE_2 = "01HZX0000000000000001BF002";
    private static final String PATIENT_1 = "01HZX00000000000000PAT0001";
    private static final String PATIENT_2 = "01HZX00000000000000PAT0002";
    private static final String ANALYSIS_GLY = "01HZX00000000000000ANA0G1Y";
    private static final String SAMPLE_1 = "01HZX00000000000000SMP0001";
    private static final String RI_GLY = "01HZX0000000000000RG1Y0001";

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 5);

    @Mock private PaillasseWorksheetRepository repository;
    @Mock private DisciplineApi disciplineApi;
    @Mock private LabFileApi labFileApi;
    @Mock private PatientApi patientApi;
    @Mock private AnalysisApi analysisApi;
    @Mock private LabSettingsApi labSettingsApi;
    @Mock private DossierCommentRepository dossierCommentRepository;

    private PaillasseWorksheetModelAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler =
                new PaillasseWorksheetModelAssembler(
                        repository,
                        disciplineApi,
                        labFileApi,
                        patientApi,
                        analysisApi,
                        labSettingsApi,
                        dossierCommentRepository,
                        FIXED_CLOCK);
        stubLabSettings();
        stubActiveDiscipline();
        when(dossierCommentRepository.findById(any())).thenReturn(Optional.empty());
        when(analysisApi.findActiveReferenceIntervalsByAnalysis(any())).thenReturn(List.of());
    }

    @Test
    void assemble_groupsRowsByDossier_inRegistrationOrder() {
        var r1 = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY", 1);
        var r2 = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY2", 2);
        var r3 = row(LAB_FILE_2, "LF-260605-0002", PATIENT_2, "GLY", "GLY", 1);
        when(repository.findRows(any(), any(), any())).thenReturn(List.of(r1, r2, r3));
        when(labFileApi.findAllByIds(anySet()))
                .thenReturn(
                        Map.of(
                                new LabFileId(LAB_FILE_1),
                                labFile(LAB_FILE_1, "LF-260605-0001", PATIENT_1, true),
                                new LabFileId(LAB_FILE_2),
                                labFile(LAB_FILE_2, "LF-260605-0002", PATIENT_2, false)));
        when(patientApi.findAllByIds(anySet()))
                .thenReturn(
                        Map.of(
                                new PatientId(PATIENT_1), patient(PATIENT_1, false, false),
                                new PatientId(PATIENT_2), patient(PATIENT_2, false, false)));

        var model = assembler.assemble(new DisciplineId(DISC_BIO_ULID), TODAY, TODAY);

        assertThat(model.dossiers()).hasSize(2);
        assertThat(model.dossiers().get(0).registrationNumber()).isEqualTo("LF-260605-0001");
        assertThat(model.dossiers().get(0).rows()).hasSize(2);
        assertThat(model.dossiers().get(0).rows().get(0).code()).isEqualTo("GLY");
        // sample on lab file 1 for GLY → dailyId resolved.
        assertThat(model.dossiers().get(0).rows().get(0).dailyId()).isEqualTo("1040626-001");
        // lab file 2 has no sample → blank.
        assertThat(model.dossiers().get(1).rows().get(0).dailyId()).isEmpty();
    }

    @Test
    void assemble_rendersEnteredValue_blankWhenNull_boldFlagFromOutOfRange() {
        var entered = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY", 1);
        when(entered.getValueReal()).thenReturn(new BigDecimal("1.45"));
        when(entered.getIsOutOfRange()).thenReturn(true);
        var blank = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY2", 2);
        when(repository.findRows(any(), any(), any())).thenReturn(List.of(entered, blank));
        stubSingleLabFileAndPatient();

        var model = assembler.assemble(new DisciplineId(DISC_BIO_ULID), TODAY, TODAY);

        var rows = model.dossiers().get(0).rows();
        assertThat(rows.get(0).valueDisplay()).isEqualTo("1.45");
        assertThat(rows.get(0).outOfRange()).isTrue();
        assertThat(rows.get(1).valueDisplay()).isEmpty();
        assertThat(rows.get(1).outOfRange()).isFalse();
    }

    @Test
    void assemble_valeursUsuelles_fromMatchedReferenceInterval() {
        var withRange = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY", 1);
        when(withRange.getReferenceIntervalId()).thenReturn(RI_GLY);
        var withComment = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY2", 2);
        var commentRiId = "01HZX0000000000000RG1Y0002";
        when(withComment.getReferenceIntervalId()).thenReturn(commentRiId);
        var noRi = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY3", 3);
        when(repository.findRows(any(), any(), any()))
                .thenReturn(List.of(withRange, withComment, noRi));
        when(analysisApi.findActiveReferenceIntervalsByAnalysis(new AnalysisId(ANALYSIS_GLY)))
                .thenReturn(
                        List.of(
                                referenceInterval(
                                        RI_GLY,
                                        new BigDecimal("0.70"),
                                        new BigDecimal("1.10"),
                                        null),
                                referenceInterval(commentRiId, null, null, "Voir notice")));
        stubSingleLabFileAndPatient();

        var model = assembler.assemble(new DisciplineId(DISC_BIO_ULID), TODAY, TODAY);

        var rows = model.dossiers().get(0).rows();
        assertThat(rows.get(0).referenceDisplay()).isEqualTo("0.70 – 1.10");
        assertThat(rows.get(1).referenceDisplay()).isEqualTo("Voir notice");
        assertThat(rows.get(2).referenceDisplay()).isEmpty();
    }

    @Test
    void assemble_techValidationFlag_fromResultState() {
        var techValidated = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY", 1);
        when(techValidated.getResultState()).thenReturn("TECHNICAL_VALIDATED");
        var bioValidated = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY2", 2);
        when(bioValidated.getResultState()).thenReturn("BIOLOGICAL_VALIDATED");
        var entered = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY3", 3);
        when(entered.getResultState()).thenReturn("ENTERED");
        var draft = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY4", 4);
        when(draft.getResultState()).thenReturn("DRAFT");
        when(repository.findRows(any(), any(), any()))
                .thenReturn(List.of(techValidated, bioValidated, entered, draft));
        stubSingleLabFileAndPatient();

        var rows =
                assembler
                        .assemble(new DisciplineId(DISC_BIO_ULID), TODAY, TODAY)
                        .dossiers()
                        .get(0)
                        .rows();

        assertThat(rows.get(0).techValidated()).isTrue();
        assertThat(rows.get(1).techValidated()).isTrue();
        assertThat(rows.get(2).techValidated()).isFalse();
        assertThat(rows.get(3).techValidated()).isFalse();
    }

    @Test
    void assemble_patientType_assuré_partenaire_orEmpty() {
        var r1 = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY", 1);
        var r2 = row(LAB_FILE_2, "LF-260605-0002", PATIENT_2, "GLY", "GLY", 1);
        var r3 =
                row(
                        "01HZX0000000000000001BF003",
                        "LF-260605-0003",
                        "01HZX00000000000000PAT0003",
                        "GLY",
                        "GLY",
                        1);
        when(repository.findRows(any(), any(), any())).thenReturn(List.of(r1, r2, r3));
        when(labFileApi.findAllByIds(anySet()))
                .thenReturn(
                        Map.of(
                                new LabFileId(LAB_FILE_1),
                                labFile(LAB_FILE_1, "LF-260605-0001", PATIENT_1, false),
                                new LabFileId(LAB_FILE_2),
                                labFile(LAB_FILE_2, "LF-260605-0002", PATIENT_2, false),
                                new LabFileId("01HZX0000000000000001BF003"),
                                labFile(
                                        "01HZX0000000000000001BF003",
                                        "LF-260605-0003",
                                        "01HZX00000000000000PAT0003",
                                        false)));
        when(patientApi.findAllByIds(anySet()))
                .thenReturn(
                        Map.of(
                                new PatientId(PATIENT_1),
                                patient(PATIENT_1, true, false),
                                new PatientId(PATIENT_2),
                                patient(PATIENT_2, false, true),
                                new PatientId("01HZX00000000000000PAT0003"),
                                patient("01HZX00000000000000PAT0003", false, false)));

        var model = assembler.assemble(new DisciplineId(DISC_BIO_ULID), TODAY, TODAY);

        assertThat(model.dossiers().get(0).patientType()).isEqualTo("Assuré");
        assertThat(model.dossiers().get(1).patientType()).isEqualTo("Partenaire");
        assertThat(model.dossiers().get(2).patientType()).isEmpty();
    }

    @Test
    void assemble_unknownDiscipline_throwsEntityNotFound() {
        when(disciplineApi.findById(new DisciplineId(DISC_BIO_ULID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assembler.assemble(new DisciplineId(DISC_BIO_ULID), TODAY, TODAY))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void assemble_softDeletedDiscipline_throwsEntityNotFound() {
        when(disciplineApi.findById(new DisciplineId(DISC_BIO_ULID)))
                .thenReturn(Optional.of(discipline(false)));

        assertThatThrownBy(() -> assembler.assemble(new DisciplineId(DISC_BIO_ULID), TODAY, TODAY))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void assemble_dossierComment_attachedWhenPresent() {
        var r1 = row(LAB_FILE_1, "LF-260605-0001", PATIENT_1, "GLY", "GLY", 1);
        when(repository.findRows(any(), any(), any())).thenReturn(List.of(r1));
        stubSingleLabFileAndPatient();
        var comment = DossierComment.create(new LabFileId(LAB_FILE_1), "Hémolyse légère");
        when(dossierCommentRepository.findById(LAB_FILE_1)).thenReturn(Optional.of(comment));

        var model = assembler.assemble(new DisciplineId(DISC_BIO_ULID), TODAY, TODAY);

        assertThat(model.dossiers().get(0).dossierComment()).isEqualTo("Hémolyse légère");
    }

    @Test
    void assemble_headerAndPeriod() {
        when(repository.findRows(any(), any(), any())).thenReturn(List.of());
        when(labFileApi.findAllByIds(anySet())).thenReturn(Map.of());
        when(patientApi.findAllByIds(anySet())).thenReturn(Map.of());

        var model = assembler.assemble(new DisciplineId(DISC_BIO_ULID), TODAY, TODAY);

        assertThat(model.paillasseName()).isEqualTo("Biochimie");
        assertThat(model.disciplineCode()).isEqualTo("BIOCHIMIE");
        assertThat(model.fromIso()).isEqualTo("2026-06-05");
        assertThat(model.toIso()).isEqualTo("2026-06-05");
        assertThat(model.periodDisplay()).isEqualTo("du 05/06/2026 au 05/06/2026");
        assertThat(model.printedAtDisplay()).isEqualTo("05/06/2026 10:00");
    }

    // ----- fixture helpers ------------------------------------------------------------------

    private void stubLabSettings() {
        when(labSettingsApi.get())
                .thenReturn(
                        new LabSettings(
                                "Lab Sparkcorp",
                                "12 Avenue du Test, Lomé",
                                "ACC-2026-001",
                                "Dr. Manager",
                                Instant.parse("2026-06-05T00:00:00Z")));
    }

    private void stubActiveDiscipline() {
        when(disciplineApi.findById(new DisciplineId(DISC_BIO_ULID)))
                .thenReturn(Optional.of(discipline(true)));
    }

    private void stubSingleLabFileAndPatient() {
        when(labFileApi.findAllByIds(anySet()))
                .thenReturn(
                        Map.of(
                                new LabFileId(LAB_FILE_1),
                                labFile(LAB_FILE_1, "LF-260605-0001", PATIENT_1, true)));
        when(patientApi.findAllByIds(anySet()))
                .thenReturn(Map.of(new PatientId(PATIENT_1), patient(PATIENT_1, false, false)));
    }

    private static DisciplineSummary discipline(boolean active) {
        return new DisciplineSummary(
                new DisciplineId(DISC_BIO_ULID),
                "BIOCHIMIE",
                "Biochimie",
                null,
                active,
                DisciplinePrefix.BIOCHIMIE);
    }

    private static PaillasseWorksheetRowProjection row(
            String labFileId,
            String registrationNumber,
            String patientId,
            String analysisCode,
            String componentCode,
            int position) {
        var p = org.mockito.Mockito.mock(PaillasseWorksheetRowProjection.class);
        when(p.getLabFileId()).thenReturn(labFileId);
        when(p.getRegistrationNumber()).thenReturn(registrationNumber);
        when(p.getPatientId()).thenReturn(patientId);
        when(p.getAnalysisCode()).thenReturn(analysisCode);
        when(p.getComponentCode()).thenReturn(componentCode);
        when(p.getAnalysisId()).thenReturn(ANALYSIS_GLY);
        when(p.getResultState()).thenReturn("ENTERED");
        when(p.getResultType()).thenReturn("REAL");
        when(p.getPosition()).thenReturn(position);
        when(p.getIsOutOfRange()).thenReturn(false);
        return p;
    }

    private static ReferenceIntervalSummary referenceInterval(
            String id, BigDecimal min, BigDecimal max, String commentText) {
        return new ReferenceIntervalSummary(
                new ReferenceIntervalId(id),
                new AnalysisId(ANALYSIS_GLY),
                PatientCategory.HOMME,
                null,
                null,
                null,
                null,
                ReferenceMode.STANDARD,
                min,
                max,
                null,
                null,
                commentText);
    }

    private static LabFileSummary labFile(
            String labFileId, String registrationNumber, String patientId, boolean withSample) {
        return new LabFileSummary(
                new LabFileId(labFileId),
                new LabFileRegistrationNumber(registrationNumber),
                new LabFileSummary.PatientSnapshot(patientId, "PAT", "DOE", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                withSample
                        ? List.of(
                                new LabFileSummary.InLabSampleSummary(
                                        new SampleId(SAMPLE_1),
                                        new SampleDailyId("1040626-001"),
                                        "GLY",
                                        "TUBE-F",
                                        Instant.parse("2026-06-05T08:00:00Z"),
                                        null))
                        : List.of(),
                List.of(),
                null,
                null,
                new LabFileSummary.BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                Instant.parse("2026-06-05T08:00:00Z"),
                null,
                null,
                null);
    }

    private static PatientSummary patient(String patientId, boolean insured, boolean partner) {
        return new PatientSummary(
                new PatientId(patientId),
                "PAT",
                "DOE",
                "Jane",
                Civility.MADAME,
                Sex.F,
                LocalDate.of(1985, 7, 12),
                insured
                        ? new PatientSummary.InsuranceSnapshot(
                                new org.sparkcorporation.lims.catalog.api.InsuranceId(
                                        "01HZX0000000000000NS000001"),
                                "INS",
                                "CNSS")
                        : null,
                partner
                        ? new PatientSummary.PartnerSnapshot(
                                new org.sparkcorporation.lims.catalog.api.PartnerId(
                                        "01HZX0000000000000PRT00001"),
                                "PRT",
                                "Clinique",
                                org.sparkcorporation.lims.catalog.api.PartnerKind.SOCIETY)
                        : null,
                false,
                false);
    }
}
