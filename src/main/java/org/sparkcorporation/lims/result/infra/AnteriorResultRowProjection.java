package org.sparkcorporation.lims.result.infra;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Native-query projection for the §IV anterior-results lookup. The mapper layer ({@code
 * AnteriorResultMapper}) converts these flat columns into {@code AnteriorResultRow}.
 *
 * <p>Methods follow JavaBean getter naming convention (required by Spring Data JPA native-query
 * interface projection resolution via {@code BeanUtils.findPropertyForMethod}).
 */
public interface AnteriorResultRowProjection {

    String getResultId();

    String getLineId();

    String getLabFileId();

    String getLabFileRegistrationNumber();

    String getComponentCode();

    String getResultType();

    Long getValueInteger();

    BigDecimal getValueReal();

    String getValueText();

    String getUnit1Snapshot();

    boolean isOutOfRange();

    Instant getBiologicalValidatedAt();
}
