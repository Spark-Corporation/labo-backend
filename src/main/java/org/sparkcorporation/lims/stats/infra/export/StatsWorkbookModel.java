package org.sparkcorporation.lims.stats.infra.export;

import java.time.LocalDate;
import java.util.List;

import org.sparkcorporation.lims.stats.infra.ActivityRow;
import org.sparkcorporation.lims.stats.infra.AnalysisStatRow;
import org.sparkcorporation.lims.stats.infra.AntibiogramStatRow;
import org.sparkcorporation.lims.stats.infra.DisciplineStatRow;
import org.sparkcorporation.lims.stats.infra.PatientStatsRow;
import org.sparkcorporation.lims.stats.infra.PrescriberStatRow;

/** All six SP1 statistics datasets for a date range — the model for the stats XLSX workbook. */
public record StatsWorkbookModel(
        LocalDate from,
        LocalDate to,
        ActivityRow activity,
        List<DisciplineStatRow> byDiscipline,
        List<PrescriberStatRow> byPrescriber,
        List<AnalysisStatRow> analyses,
        List<AntibiogramStatRow> antibiograms,
        PatientStatsRow patients) {}
