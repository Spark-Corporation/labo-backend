package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
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
import org.sparkcorporation.lims.order.domain.NewLabFile;
import org.sparkcorporation.lims.order.domain.Sample;
import org.sparkcorporation.lims.order.infra.DefaultSampleDailyIdGenerator;
import org.sparkcorporation.lims.order.infra.LabFileRepository;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Verifies SP9 conservation snapshotting at sample collection: {@code recordInLab} copies the
 * analysis conservation defaults onto the sample and computes the destruction-due instant.
 */
@ExtendWith(MockitoExtension.class)
class SampleServiceConservationTest {

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

    private static AnalysisSummary analysisSummary(
            @Nullable String defaultPortoir, @Nullable Integer defaultConservationDays) {
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
                defaultPortoir,
                defaultConservationDays,
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
                null,
                null);
    }

    private Sample recordWith(LabFile file, AnalysisSummary summary) {
        when(fileRepo.findActiveById(VALID_ULID)).thenReturn(Optional.of(file));
        when(analysisApi.findById(any(AnalysisId.class))).thenReturn(Optional.of(summary));
        var dailyId = new SampleDailyId("1070526-001");
        when(dailyIdGen.next(eq(DisciplinePrefix.BIOCHIMIE.prefix()), eq(LocalDate.of(2026, 5, 7))))
                .thenReturn(dailyId);
        when(labFileApi.requireById(new LabFileId(VALID_ULID)))
                .thenAnswer(
                        inv -> {
                            var s = file.samplesView().get(0);
                            return fileSummaryWithSample(s.getDailyId(), s.rawId());
                        });
        var req = new RecordInLabSampleRequest(VALID_ULID, VALID_ULID, FIXED_NOW);
        service.recordInLab(new LabFileId(VALID_ULID), req, "user-1");
        return file.samplesView().get(0);
    }

    @Test
    void recordInLab_snapshots_conservation_and_computes_due_date() {
        var file = newOpenFile();
        var sample = recordWith(file, analysisSummary("P07", 14));

        assertThat(sample.getPortoir()).isEqualTo("P07");
        assertThat(sample.getConservationDays()).isEqualTo(14);
        assertThat(sample.getConservationStart()).isEqualTo(FIXED_NOW);
        assertThat(sample.getDestructionDueAt()).isEqualTo(FIXED_NOW.plus(14, ChronoUnit.DAYS));
    }

    @Test
    void recordInLab_with_null_conservation_days_leaves_due_date_null() {
        var file = newOpenFile();
        var sample = recordWith(file, analysisSummary("P07", null));

        assertThat(sample.getConservationDays()).isNull();
        assertThat(sample.getDestructionDueAt()).isNull();
    }
}
