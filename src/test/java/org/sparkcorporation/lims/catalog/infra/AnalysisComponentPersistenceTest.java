package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.NewAnalysis;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

/**
 * Slice test for {@link Analysis} ↔ {@link
 * org.sparkcorporation.lims.catalog.domain.AnalysisComponent} parent-child round-trip persistence.
 * Verifies that ordering (by {@code position ASC}) survives a save/load cycle.
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class AnalysisComponentPersistenceTest extends IntegrationTestBase {

    @Autowired private AnalysisRepository analyses;
    @Autowired private DisciplineRepository disciplines;

    private Analysis simple(String code, Discipline d) {
        var cmd =
                new NewAnalysis(
                        code,
                        code,
                        AnalysisKind.SIMPLE,
                        new BigDecimal("10.00"),
                        1,
                        d.id(),
                        null,
                        null,
                        ResultType.REAL,
                        null,
                        null,
                        null,
                        null,
                        null);
        return Analysis.create(cmd, d, null, null, Clock.systemUTC());
    }

    private Analysis grouped(String code, Discipline d) {
        var cmd =
                new NewAnalysis(
                        code,
                        code,
                        AnalysisKind.GROUPED,
                        new BigDecimal("10.00"),
                        1,
                        d.id(),
                        null,
                        null,
                        ResultType.REAL,
                        null,
                        null,
                        null,
                        null,
                        null);
        return Analysis.create(cmd, d, null, null, Clock.systemUTC());
    }

    @Test
    void parent_with_components_round_trip_preserves_order_by_position() {
        var d =
                disciplines.save(
                        Discipline.create("COMP_DISC_RT", "D", null, DisciplinePrefix.BIOCHIMIE));
        var parent = analyses.save(grouped("COMP_PARENT", d));
        var c0 = analyses.save(simple("COMP_CHILD_0", d));
        var c1 = analyses.save(simple("COMP_CHILD_1", d));
        var c2 = analyses.save(simple("COMP_CHILD_2", d));
        analyses.flush();

        // Add in non-sequential order to verify @OrderBy("position ASC") sorts on load.
        parent.addComponent(c2, 2);
        parent.addComponent(c0, 0);
        parent.addComponent(c1, 1);
        analyses.saveAndFlush(parent);

        var rows = analyses.findChildrenForParent(parent.rawId());
        assertThat(rows).hasSize(3);
        assertThat((String) rows.get(0)[1]).isEqualTo("COMP_CHILD_0");
        assertThat((String) rows.get(1)[1]).isEqualTo("COMP_CHILD_1");
        assertThat((String) rows.get(2)[1]).isEqualTo("COMP_CHILD_2");
        assertThat(((Number) rows.get(0)[3]).intValue()).isZero();
        assertThat(((Number) rows.get(1)[3]).intValue()).isOne();
        assertThat(((Number) rows.get(2)[3]).intValue()).isEqualTo(2);
    }
}
