package org.sparkcorporation.lims.catalog.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.sparkcorporation.lims.catalog.domain.NewPartner;
import org.sparkcorporation.lims.catalog.domain.UpdatePartner;

/** Cross-module CRUD interface for the {@code Partner} aggregate. Mirrors {@link InsuranceApi}. */
public interface PartnerApi {

    PartnerSummary create(NewPartner req);

    PartnerSummary update(PartnerId id, UpdatePartner req);

    void disable(PartnerId id);

    Optional<PartnerSummary> findById(PartnerId id);

    Map<PartnerId, PartnerSummary> findAllByIds(Collection<PartnerId> ids);

    boolean exists(PartnerId id);

    Page<PartnerSummary> search(
            @Nullable String q,
            @Nullable PartnerKind kind,
            @Nullable Boolean disabled,
            Pageable pageable);
}
