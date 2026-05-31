package org.sparkcorporation.lims.stats.infra.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.sparkcorporation.lims.stats.infra.ActivityRow;
import org.sparkcorporation.lims.stats.infra.PatientStatsRow;
import org.sparkcorporation.lims.stats.infra.StatsReadModel;

class StatsWorkbookAssemblerTest {

    private final StatsReadModel readModel = Mockito.mock(StatsReadModel.class);
    private final StatsWorkbookAssembler assembler = new StatsWorkbookAssembler(readModel);

    @Test
    void assembles_all_six_datasets_unfiltered() {
        var from = LocalDate.parse("2026-05-01");
        var to = LocalDate.parse("2026-05-31");
        when(readModel.activity(from, to, null, null, null, null))
                .thenReturn(new ActivityRow(10, 2, 8, 9, 3));
        when(readModel.byDiscipline(from, to, null, null)).thenReturn(List.of());
        when(readModel.byPrescriber(from, to, null)).thenReturn(List.of());
        when(readModel.analyses(from, to, null)).thenReturn(List.of());
        when(readModel.antibiograms(from, to, null)).thenReturn(List.of());
        when(readModel.patients(from, to, null, null))
                .thenReturn(new PatientStatsRow(9, 3, Map.of(), Map.of()));

        var model = assembler.assemble(from, to);

        assertThat(model.activity().dossiers()).isEqualTo(10);
        assertThat(model.from()).isEqualTo(from);
        assertThat(model.to()).isEqualTo(to);
        verify(readModel).antibiograms(from, to, null);
    }
}
