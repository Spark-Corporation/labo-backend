package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.api.SampleDailyId;
import org.sparkcorporation.lims.order.domain.LabFile;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.NewLabFile;
import org.sparkcorporation.lims.order.domain.OrderInvalidAnalysisReferenceException;
import org.sparkcorporation.lims.order.infra.DefaultSampleDailyIdGenerator;
import org.sparkcorporation.lims.order.infra.LabFileRepository;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.Sex;

@ExtendWith(MockitoExtension.class)
class SampleServiceTest {

    private static final String VALID_ULID = "01HZB0000000000000000ADMNZ";
    private static final Instant FIXED_NOW = Instant.parse("2026-05-07T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock private LabFileRepository fileRepo;
    @Mock private LabFileApi labFileApi;
    @Mock private AnalysisApi analysisApi;
    @Mock private DefaultSampleDailyIdGenerator dailyIdGen;

    private SampleService service;

    @BeforeEach
    void setUp() {
        service = new SampleService(fileRepo, labFileApi, analysisApi, dailyIdGen, FIXED_CLOCK);
    }

    private static AnalysisSummary analysisSummary() {
        return new AnalysisSummary(
                AnalysisId.generate(),
                "GLU",
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

    private static LabFile newOpenFile() {
        return LabFile.open(
                new NewLabFile(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of(),
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null),
                new LabFileRegistrationNumber("LF-260507-0001"),
                "user-1",
                FIXED_CLOCK);
    }

    private static LabFileSummary fileSummaryWithSample(SampleDailyId dailyId, String sampleId) {
        return new LabFileSummary(
                new LabFileId(VALID_ULID),
                new LabFileRegistrationNumber("LF-260507-0001"),
                new LabFileSummary.PatientSnapshot(VALID_ULID, "P001", "Doe", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                List.of(
                        new LabFileSummary.InLabSampleSummary(
                                new org.sparkcorporation.lims.order.api.SampleId(sampleId),
                                dailyId,
                                "GLU",
                                "TUBE_SEC",
                                FIXED_NOW,
                                null)),
                List.of(),
                null,
                null,
                new LabFileSummary.BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                FIXED_NOW,
                null,
                null,
                null);
    }

    @Test
    void recordInLab_throws_when_file_not_found() {
        when(fileRepo.findActiveById(VALID_ULID)).thenReturn(Optional.empty());
        var req = new RecordInLabSampleRequest(VALID_ULID, VALID_ULID, FIXED_NOW);
        assertThatThrownBy(() -> service.recordInLab(new LabFileId(VALID_ULID), req, "user-1"))
                .isInstanceOf(LabFileNotFoundException.class);
        verify(dailyIdGen, never()).next(anyInt(), any(LocalDate.class));
    }

    @Test
    void recordInLab_throws_on_unknown_analysis() {
        when(fileRepo.findActiveById(VALID_ULID)).thenReturn(Optional.of(newOpenFile()));
        when(analysisApi.findById(any(AnalysisId.class))).thenReturn(Optional.empty());
        var req = new RecordInLabSampleRequest(VALID_ULID, VALID_ULID, FIXED_NOW);
        assertThatThrownBy(() -> service.recordInLab(new LabFileId(VALID_ULID), req, "user-1"))
                .isInstanceOf(OrderInvalidAnalysisReferenceException.class);
        verify(dailyIdGen, never()).next(anyInt(), any(LocalDate.class));
    }

    /** Spec §5.3.1: discipline prefix derives from {@code analysis.discipline().prefix()}. */
    @Test
    void recordInLab_derives_discipline_prefix_from_analysis_summary() {
        var file = newOpenFile();
        when(fileRepo.findActiveById(VALID_ULID)).thenReturn(Optional.of(file));
        when(analysisApi.findById(any(AnalysisId.class)))
                .thenReturn(Optional.of(analysisSummary()));
        var dailyId = new SampleDailyId("1070526-001");
        when(dailyIdGen.next(eq(DisciplinePrefix.BIOCHIMIE.prefix()), eq(LocalDate.of(2026, 5, 7))))
                .thenReturn(dailyId);

        // Set up labFileApi.requireById to find the new sample (we don't know its id yet, so
        // return a summary with any sample — the service stream-filters by sample id, so we
        // satisfy by returning the file's actual sample collection).
        // Easier: capture the sample after addSample and surface it via the labFileApi.
        when(labFileApi.requireById(new LabFileId(VALID_ULID)))
                .thenAnswer(
                        inv -> {
                            var sample = file.samplesView().get(0);
                            return fileSummaryWithSample(sample.getDailyId(), sample.rawId());
                        });

        var req = new RecordInLabSampleRequest(VALID_ULID, VALID_ULID, FIXED_NOW);
        var resp = service.recordInLab(new LabFileId(VALID_ULID), req, "user-1");

        org.assertj.core.api.Assertions.assertThat(resp).isInstanceOf(SampleResponse.InLab.class);
        verify(dailyIdGen).next(DisciplinePrefix.BIOCHIMIE.prefix(), LocalDate.of(2026, 5, 7));
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
