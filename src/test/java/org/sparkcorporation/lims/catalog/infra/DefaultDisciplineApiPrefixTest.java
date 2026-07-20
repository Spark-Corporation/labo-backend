package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

/**
 * Pins §3.0 amendment #2 — {@code DisciplineSummary} carries the {@code prefix} field populated
 * from the {@code Discipline} aggregate's {@code disciplinePrefix} value at read time.
 */
class DefaultDisciplineApiPrefixTest {

    private final DisciplineRepository repo = mock(DisciplineRepository.class);
    private final DefaultDisciplineApi api = new DefaultDisciplineApi(repo);

    @Test
    void find_by_id_returns_summary_with_prefix() {
        var d = Discipline.create("HEMA", "Hématologie", null, DisciplinePrefix.HEMATOLOGIE);
        // DefaultDisciplineApi.findById calls repo.findById(...) (catalog Discipline uses
        // @SQLDelete only, no @SQLRestriction; soft-deleted disciplines are returned and the
        // snapshot precedent surfaces them — see §6.2 of the spec).
        when(repo.findById(d.rawId())).thenReturn(Optional.of(d));
        var summary = api.findById(d.id()).orElseThrow();
        assertThat(summary.prefix()).isEqualTo(DisciplinePrefix.HEMATOLOGIE);
    }
}
