package org.sparkcorporation.lims.catalog.infra;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.InsuranceApi;
import org.sparkcorporation.lims.catalog.api.InsuranceCreated;
import org.sparkcorporation.lims.catalog.api.InsuranceDisabled;
import org.sparkcorporation.lims.catalog.api.InsuranceId;
import org.sparkcorporation.lims.catalog.api.InsuranceSummary;
import org.sparkcorporation.lims.catalog.api.InsuranceUpdated;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Insurance;
import org.sparkcorporation.lims.catalog.domain.InsuranceNotFoundException;
import org.sparkcorporation.lims.catalog.domain.NewInsurance;
import org.sparkcorporation.lims.catalog.domain.UpdateInsurance;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultInsuranceApi implements InsuranceApi {

    private final InsuranceRepository repo;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public InsuranceSummary create(NewInsurance req) {
        if (repo.existsByCode(req.code())) {
            throw new CodeAlreadyExistsException("Insurance", req.code());
        }
        var insurance = Insurance.create(req, clock);
        repo.save(insurance);
        events.publishEvent(
                new InsuranceCreated(insurance.id(), insurance.code(), Instant.now(clock)));
        return toSummary(insurance);
    }

    @Override
    @Transactional
    public InsuranceSummary update(InsuranceId id, UpdateInsurance req) {
        var insurance =
                repo.findById(id.value()).orElseThrow(() -> new InsuranceNotFoundException(id));
        insurance.update(req, clock);
        events.publishEvent(new InsuranceUpdated(insurance.id(), Instant.now(clock)));
        return toSummary(insurance);
    }

    @Override
    @Transactional
    public void disable(InsuranceId id) {
        var insurance = repo.findById(id.value()).orElse(null);
        if (insurance == null) {
            // Idempotent: already disabled (filtered by @SQLRestriction) or never existed.
            return;
        }
        insurance.disable(clock);
        events.publishEvent(new InsuranceDisabled(insurance.id(), Instant.now(clock)));
    }

    @Override
    public Optional<InsuranceSummary> findById(InsuranceId id) {
        return repo.findById(id.value()).map(DefaultInsuranceApi::toSummary);
    }

    @Override
    public Map<InsuranceId, InsuranceSummary> findAllByIds(Collection<InsuranceId> ids) {
        Set<String> rawIds = new HashSet<>();
        for (InsuranceId id : ids) {
            rawIds.add(id.value());
        }
        Map<InsuranceId, InsuranceSummary> result = new HashMap<>();
        for (Insurance i : repo.findAllById(rawIds)) {
            var summary = toSummary(i);
            result.put(summary.id(), summary);
        }
        return result;
    }

    @Override
    public boolean exists(InsuranceId id) {
        return repo.existsById(id.value());
    }

    @Override
    public Page<InsuranceSummary> search(
            @Nullable String q, @Nullable Boolean disabled, Pageable pageable) {
        return repo.searchNative(q, disabled, pageable).map(DefaultInsuranceApi::toSummary);
    }

    private static InsuranceSummary toSummary(Insurance i) {
        return new InsuranceSummary(
                i.id(),
                i.code(),
                i.name(),
                i.responsibleEmail(),
                i.responsibleContact(),
                i.bRate(),
                i.coverageRate(),
                i.isDisabled());
    }
}
