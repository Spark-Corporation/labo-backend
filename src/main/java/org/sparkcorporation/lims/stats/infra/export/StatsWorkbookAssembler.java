package org.sparkcorporation.lims.stats.infra.export;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.stats.infra.StatsReadModel;

/** Bundles the six SP1 datasets (unfiltered, date-range only) for the XLSX export. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsWorkbookAssembler {

    private final StatsReadModel readModel;

    public StatsWorkbookModel assemble(LocalDate from, LocalDate to) {
        return new StatsWorkbookModel(
                from,
                to,
                readModel.activity(from, to, null, null, null, null),
                readModel.byDiscipline(from, to, null, null),
                readModel.byPrescriber(from, to, null),
                readModel.analyses(from, to, null),
                readModel.antibiograms(from, to, null),
                readModel.patients(from, to, null, null));
    }
}
