package org.sparkcorporation.lims.result.web;

import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.infra.AnteriorResultRowProjection;

/**
 * Static, stateless mapper from the native-query projection to the API {@link AnteriorResultRow}.
 */
public final class AnteriorResultMapper {

    private AnteriorResultMapper() {
        throw new AssertionError("utility class");
    }

    public static AnteriorResultRow toRow(AnteriorResultRowProjection projection) {
        return new AnteriorResultRow(
                new ResultId(projection.getResultId()),
                new ResultLineId(projection.getLineId()),
                new LabFileId(projection.getLabFileId()),
                projection.getLabFileRegistrationNumber(),
                projection.getComponentCode(),
                ResultType.valueOf(projection.getResultType()),
                projection.getValueInteger(),
                projection.getValueReal(),
                projection.getValueText(),
                projection.getUnit1Snapshot(),
                projection.isOutOfRange(),
                projection.getBiologicalValidatedAt());
    }
}
