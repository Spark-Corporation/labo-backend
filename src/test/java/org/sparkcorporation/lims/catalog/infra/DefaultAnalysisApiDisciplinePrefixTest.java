package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.NewAnalysis;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

/**
 * Pins §3.0 amendment #3 — {@code AnalysisSummary.DisciplineSnapshot.prefix} is populated from the
 * underlying {@code Discipline}'s {@code disciplinePrefix} so the order module can compose daily
 * IDs without an extra catalog round-trip.
 */
class DefaultAnalysisApiDisciplinePrefixTest {

    private final AnalysisRepository repo = mock(AnalysisRepository.class);
    private final DefaultAnalysisApi api = new DefaultAnalysisApi(repo);

    @Test
    void find_by_code_returns_summary_with_discipline_snapshot_prefix() {
        var disc = Discipline.create("BIO", "Biochimie", null, DisciplinePrefix.BIOCHIMIE);
        var cmd =
                new NewAnalysis(
                        "GLY",
                        "Glycémie",
                        AnalysisKind.SIMPLE,
                        new BigDecimal("10.00"),
                        1,
                        disc.id(),
                        null,
                        null,
                        ResultType.REAL,
                        null,
                        null,
                        null,
                        null,
                        null);
        var analysis = Analysis.create(cmd, disc, null, null, Clock.systemUTC());
        when(repo.findByCode("GLY")).thenReturn(Optional.of(analysis));
        when(repo.findChildrenForParent(any())).thenReturn(List.of());
        when(repo.findTechniqueLinks(any())).thenReturn(List.of());

        var summary = api.findByCode(new AnalysisCode("GLY")).orElseThrow();
        assertThat(summary.discipline().prefix()).isEqualTo(DisciplinePrefix.BIOCHIMIE);
    }
}
