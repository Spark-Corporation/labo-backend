package org.sparkcorporation.lims.order.infra.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.RecipientId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.labsettings.api.LabSettings;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
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
class WorksheetModelAssemblerTest {

    // Valid 26-char Crockford Base32 ULIDs (alphabet excludes I, L, O, U).
    private static final String LAB_FILE_ULID = "01HZX0000000000000001BF001";
    private static final String PATIENT_ULID = "01HZX00000000000000PAT0001";
    private static final String ANALYSIS_GLY = "01HZX00000000000000ANA0G1Y";
    private static final String ANALYSIS_CRE = "01HZX00000000000000ANA0CRE";
    private static final String ANALYSIS_NFS = "01HZX00000000000000ANA0NFS";
    private static final String SAMPLE_ULID = "01HZX00000000000000SMP0001";
    private static final String DISC_BIO_ULID = "01HZX0000000000000DSCB10C1";
    private static final String DISC_HEM_ULID = "01HZX0000000000000DSCHEM21";
    private static final String RECIP_ULID = "01HZX0000000000000RECP0021";
    private static final String PRELEVEUR_ULID = "01HZX0000000000000PREV0V21";

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T10:00:00Z"), ZoneOffset.UTC);

    @Mock private LabFileApi labFileApi;
    @Mock private PatientApi patientApi;
    @Mock private AnalysisApi analysisApi;
    @Mock private LabSettingsApi labSettingsApi;

    private WorksheetModelAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler =
                new WorksheetModelAssembler(
                        labFileApi, patientApi, analysisApi, labSettingsApi, FIXED_CLOCK);
        stubLabSettings();
    }

    @Test
    void assemble_groupsAnalysesByPaillasse_sortedByName() {
        when(labFileApi.requireById(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(labFileWithThreeAnalyses(false, true));
        stubPatient(LocalDate.of(1985, 7, 12));
        stubAnalyses();

        var model = assembler.assemble(new LabFileId(LAB_FILE_ULID));

        // BIOCHIMIE (GLY, CREAT) then HEMATOLOGIE (NFS), sorted by paillasse name.
        assertThat(model.paillasses()).hasSize(2);
        assertThat(model.paillasses().get(0).paillasseName()).isEqualTo("Biochimie");
        assertThat(model.paillasses().get(0).rows()).hasSize(2);
        assertThat(model.paillasses().get(0).rows())
                .extracting(WorksheetModel.Row::code)
                .containsExactly("GLY", "CREAT");
        assertThat(model.paillasses().get(0).rows().get(0).analysisName()).isEqualTo("Glucose");
        assertThat(model.paillasses().get(0).rows().get(0).recipientName()).isEqualTo("Tube EDTA");
        assertThat(model.paillasses().get(1).paillasseName()).isEqualTo("Hématologie");
        assertThat(model.paillasses().get(1).rows())
                .extracting(WorksheetModel.Row::code)
                .containsExactly("NFS");
    }

    @Test
    void assemble_mapsSampleDailyIdToMatchingAnalysisRow_blankWhenAbsent() {
        when(labFileApi.requireById(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(labFileWithThreeAnalyses(false, true));
        stubPatient(LocalDate.of(1985, 7, 12));
        stubAnalyses();

        var model = assembler.assemble(new LabFileId(LAB_FILE_ULID));

        var glyRow =
                model.paillasses().get(0).rows().stream()
                        .filter(r -> r.code().equals("GLY"))
                        .findFirst()
                        .orElseThrow();
        assertThat(glyRow.dailyId()).isEqualTo("1040626-001");
        var creatRow =
                model.paillasses().get(0).rows().stream()
                        .filter(r -> r.code().equals("CREAT"))
                        .findFirst()
                        .orElseThrow();
        assertThat(creatRow.dailyId()).isEmpty();
    }

    @Test
    void assemble_header_carriesRegistrationNumberUrgentAndPatient() {
        when(labFileApi.requireById(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(labFileWithThreeAnalyses(true, true));
        stubPatient(LocalDate.of(1985, 7, 12));
        stubAnalyses();

        var model = assembler.assemble(new LabFileId(LAB_FILE_ULID));

        assertThat(model.header().registrationNumber()).isEqualTo("LF-260605-0001");
        assertThat(model.header().urgent()).isTrue();
        assertThat(model.header().createdAtDisplay()).isEqualTo("05/06/2026");
        assertThat(model.patient().nom()).isEqualTo("DOE");
        assertThat(model.patient().prenom()).isEqualTo("Jane");
        assertThat(model.patient().sexe()).isEqualTo("F");
        assertThat(model.patient().age()).isNotBlank();
    }

    @Test
    void assemble_footer_printedAtFromClock_preleveurWhenPresent() {
        when(labFileApi.requireById(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(labFileWithThreeAnalyses(false, true));
        stubPatient(LocalDate.of(1985, 7, 12));
        stubAnalyses();

        var withPreleveur = assembler.assemble(new LabFileId(LAB_FILE_ULID));

        assertThat(withPreleveur.footer().printedAtDisplay()).isEqualTo("05/06/2026 10:00");
        assertThat(withPreleveur.footer().preleveurName()).isEqualTo("AGBE Komla");

        when(labFileApi.requireById(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(labFileWithThreeAnalyses(false, false));
        var withoutPreleveur = assembler.assemble(new LabFileId(LAB_FILE_ULID));

        assertThat(withoutPreleveur.footer().preleveurName()).isNull();
    }

    @Test
    void assemble_propagatesLabFileNotFound() {
        var id = new LabFileId(LAB_FILE_ULID);
        when(labFileApi.requireById(id)).thenThrow(new LabFileNotFoundException(id));

        assertThatThrownBy(() -> assembler.assemble(id))
                .isInstanceOf(LabFileNotFoundException.class);
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

    private void stubPatient(LocalDate birthDate) {
        when(patientApi.requireById(new PatientId(PATIENT_ULID)))
                .thenReturn(
                        new PatientSummary(
                                new PatientId(PATIENT_ULID),
                                "PAT-260605-0001",
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
        when(analysisApi.findAllByIds(anyCollection()))
                .thenReturn(
                        Map.of(
                                new AnalysisId(ANALYSIS_GLY),
                                analysis(ANALYSIS_GLY, "GLY", "Glucose", biochimie()),
                                new AnalysisId(ANALYSIS_CRE),
                                analysis(ANALYSIS_CRE, "CREAT", "Créatinine", biochimie()),
                                new AnalysisId(ANALYSIS_NFS),
                                analysis(ANALYSIS_NFS, "NFS", "Numération", hematologie())));
    }

    private static AnalysisSummary.DisciplineSnapshot biochimie() {
        return new AnalysisSummary.DisciplineSnapshot(
                new DisciplineId(DISC_BIO_ULID),
                "BIOCHIMIE",
                "Biochimie",
                DisciplinePrefix.BIOCHIMIE);
    }

    private static AnalysisSummary.DisciplineSnapshot hematologie() {
        return new AnalysisSummary.DisciplineSnapshot(
                new DisciplineId(DISC_HEM_ULID),
                "HEMATOLOGIE",
                "Hématologie",
                DisciplinePrefix.HEMATOLOGIE);
    }

    private static AnalysisSummary analysis(
            String id, String code, String name, AnalysisSummary.DisciplineSnapshot discipline) {
        return new AnalysisSummary(
                new AnalysisId(id),
                code,
                name,
                AnalysisKind.SIMPLE,
                BigDecimal.TEN,
                1,
                discipline,
                null,
                new AnalysisSummary.RecipientSnapshot(
                        new RecipientId(RECIP_ULID), "TUBE-F", "Tube EDTA"),
                ResultType.REAL,
                null,
                null,
                null,
                new AnalysisSummary.UnitsSummary(null, null, null, null, null),
                new AnalysisSummary.AnalyticalDelaySummary(null, null, null),
                List.of(),
                List.of());
    }

    private LabFileSummary labFileWithThreeAnalyses(boolean urgent, boolean withPreleveur) {
        return new LabFileSummary(
                new LabFileId(LAB_FILE_ULID),
                new LabFileRegistrationNumber("LF-260605-0001"),
                new LabFileSummary.PatientSnapshot(
                        PATIENT_ULID, "PAT-260605-0001", "DOE", "Jane", Sex.F),
                null,
                withPreleveur
                        ? new LabFileSummary.PreleveurSnapshot(
                                PRELEVEUR_ULID, "PRE-001", "AGBE Komla")
                        : null,
                urgent,
                LabFileStatus.OPEN,
                List.of(
                        new LabFileSummary.InLabSampleSummary(
                                new SampleId(SAMPLE_ULID),
                                new SampleDailyId("1040626-001"),
                                "GLY",
                                "TUBE-F",
                                Instant.parse("2026-06-05T08:00:00Z"),
                                null)),
                List.of(
                        new LabFileSummary.AnalysisRequestSummary(
                                new AnalysisRequestId("01HZX0000000000000AREQ0001"),
                                ANALYSIS_GLY,
                                "GLY",
                                Instant.parse("2026-06-05T08:00:00Z")),
                        new LabFileSummary.AnalysisRequestSummary(
                                new AnalysisRequestId("01HZX0000000000000AREQ0002"),
                                ANALYSIS_CRE,
                                "CREAT",
                                Instant.parse("2026-06-05T08:00:00Z")),
                        new LabFileSummary.AnalysisRequestSummary(
                                new AnalysisRequestId("01HZX0000000000000AREQ0003"),
                                ANALYSIS_NFS,
                                "NFS",
                                Instant.parse("2026-06-05T08:00:00Z"))),
                null,
                null,
                new LabFileSummary.BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                Instant.parse("2026-06-05T08:00:00Z"),
                null,
                null,
                null,
                null);
    }
}
