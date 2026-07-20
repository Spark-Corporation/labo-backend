package org.sparkcorporation.lims.catalog.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.sparkcorporation.lims.catalog.domain.NewInsurance;
import org.sparkcorporation.lims.catalog.domain.UpdateInsurance;

/**
 * Cross-module CRUD interface for the {@code Insurance} aggregate. Unlike the read-only catalog
 * APIs from earlier phases, billing + patient modules need to write through this surface (lab
 * managers configure insurances via {@code POST /api/v1/insurances} and Patient validates the FK at
 * create time).
 */
public interface InsuranceApi {

    InsuranceSummary create(NewInsurance req);

    InsuranceSummary update(InsuranceId id, UpdateInsurance req);

    void disable(InsuranceId id);

    Optional<InsuranceSummary> findById(InsuranceId id);

    Map<InsuranceId, InsuranceSummary> findAllByIds(Collection<InsuranceId> ids);

    boolean exists(InsuranceId id);

    Page<InsuranceSummary> search(
            @Nullable String q, @Nullable Boolean disabled, Pageable pageable);
}
