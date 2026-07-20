package org.sparkcorporation.lims.result.infra;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Spring Data interface projection for the §X / §IX dossier queries. The native SQL in {@link
 * DossierRepository} returns columns matching these accessors; cross-module rehydration (LabFile +
 * Patient) happens in the service layer via {@code findAllByIds}.
 *
 * <p>Marker is returned as a SQL string ('GREEN' | 'RED' | 'PEN') from {@link DossierMarkerSql};
 * the service / mapper layer maps it to the {@link
 * org.sparkcorporation.lims.result.web.DossierMarker} enum.
 *
 * <p>Methods follow JavaBean getter naming convention (required by Spring Data JPA native-query
 * interface projection resolution via {@code BeanUtils.findPropertyForMethod}).
 */
public interface DossierRowProjection {

    String getLabFileId();

    String getPatientId();

    String getMarker();

    @Nullable Instant getPrintedAt();

    @Nullable Instant getEmailedAt();
}
