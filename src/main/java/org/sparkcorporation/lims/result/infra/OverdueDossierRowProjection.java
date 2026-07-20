package org.sparkcorporation.lims.result.infra;

import java.time.Instant;

/**
 * Spring Data interface projection for {@link DossierRepository#findOverdue}. Carries the existing
 * §X/§IX dossier shape plus the §"Dossiers en retard" diagnostics: the earliest-deadline analysis
 * (canary), its deadline, hours past the deadline, and the total count of overdue analyses on the
 * file.
 *
 * <p>Getter naming follows JavaBean conventions per Spring Data JPA native-query interface
 * projection resolution.
 */
public interface OverdueDossierRowProjection {

    String getLabFileId();

    String getPatientId();

    String getMarker();

    String getAnalysisCode();

    String getAnalysisName();

    Instant getDeadline();

    int getHoursPastDeadline();

    int getOverdueAnalysisCount();
}
