package org.sparkcorporation.lims.order.infra;

import java.time.Instant;

/**
 * Spring Data interface projection for {@link SampleRepository#findDestructionDue}. Carries the
 * overdue-sample shape for the destruction queue (CDC Onglet « Conservation »): the sample's daily
 * id, the snapshotted portoir / conservation start, the computed destruction deadline, days past
 * that deadline, and the parent dossier reference.
 *
 * <p>Getter naming follows JavaBean conventions per Spring Data JPA native-query interface
 * projection resolution (each getter maps to a {@code SELECT ... AS "camelCase"} alias).
 */
public interface SampleDestructionQueueProjection {

    String getSampleId();

    String getDailyId();

    String getAnalysisCode();

    String getAnalysisName();

    String getPortoir();

    Instant getConservationStart();

    Instant getDestructionDueAt();

    int getDaysOverdue();

    String getLabFileId();

    String getLabFileReference();
}
