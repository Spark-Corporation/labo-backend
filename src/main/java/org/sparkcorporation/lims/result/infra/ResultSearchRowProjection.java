package org.sparkcorporation.lims.result.infra;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Native-query projection for the §V line-level search. The mapper layer ({@code
 * ResultSearchMapper}) converts these flat columns into the API row + cross-module patient
 * hydration.
 *
 * <p>Methods follow JavaBean getter naming convention (required by Spring Data JPA native-query
 * interface projection resolution via {@code BeanUtils.findPropertyForMethod}).
 */
public interface ResultSearchRowProjection {

    String getResultId();

    String getLineId();

    String getLabFileId();

    String getLabFileRegistrationNumber();

    String getPatientId();

    String getAnalysisCode();

    String getComponentCode();

    int getPosition();

    String getResultType();

    Long getValueInteger();

    BigDecimal getValueReal();

    String getValueText();

    String getUnit1Snapshot();

    boolean isOutOfRange();

    String getReferenceIntervalId();

    String getResultState();

    Instant getBiologicalValidatedAt();

    Instant getCreatedAt();
}
