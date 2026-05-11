package org.sparkcorporation.lims.result.domain;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.ComponentLink;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Command record for {@link Result#create(NewResult, java.time.Clock)}. Carries the fully-resolved
 * analysis snapshot (kind, components, units, resultType) so the factory can materialize lines
 * without a second catalog round-trip.
 */
public record NewResult(
        LabFileId labFileId,
        AnalysisRequestId analysisRequestId,
        AnalysisId analysisId,
        String analysisCodeSnapshot,
        AnalysisKind analysisKind,
        ResultType resultType,
        List<ComponentLink> components,
        Sex patientSex,
        @Nullable Integer patientAgeInDays,
        PatientCategory patientCategory,
        @Nullable String unit1,
        @Nullable Integer unit1Decimals,
        @Nullable String unit2,
        @Nullable Integer unit2Decimals) {

    public NewResult {
        components = List.copyOf(components);
    }
}
