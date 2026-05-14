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

import org.sparkcorporation.lims.catalog.api.PartnerApi;
import org.sparkcorporation.lims.catalog.api.PartnerCreated;
import org.sparkcorporation.lims.catalog.api.PartnerDisabled;
import org.sparkcorporation.lims.catalog.api.PartnerId;
import org.sparkcorporation.lims.catalog.api.PartnerKind;
import org.sparkcorporation.lims.catalog.api.PartnerSummary;
import org.sparkcorporation.lims.catalog.api.PartnerUpdated;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.NewPartner;
import org.sparkcorporation.lims.catalog.domain.Partner;
import org.sparkcorporation.lims.catalog.domain.PartnerNotFoundException;
import org.sparkcorporation.lims.catalog.domain.UpdatePartner;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPartnerApi implements PartnerApi {

    private final PartnerRepository repo;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public PartnerSummary create(NewPartner req) {
        if (repo.existsByCode(req.code())) {
            throw new CodeAlreadyExistsException("Partner", req.code());
        }
        var partner = Partner.create(req, clock);
        repo.save(partner);
        events.publishEvent(new PartnerCreated(partner.id(), partner.code(), Instant.now(clock)));
        return toSummary(partner);
    }

    @Override
    @Transactional
    public PartnerSummary update(PartnerId id, UpdatePartner req) {
        var partner = repo.findById(id.value()).orElseThrow(() -> new PartnerNotFoundException(id));
        partner.update(req, clock);
        events.publishEvent(new PartnerUpdated(partner.id(), Instant.now(clock)));
        return toSummary(partner);
    }

    @Override
    @Transactional
    public void disable(PartnerId id) {
        var partner = repo.findById(id.value()).orElse(null);
        if (partner == null) {
            return;
        }
        partner.disable(clock);
        events.publishEvent(new PartnerDisabled(partner.id(), Instant.now(clock)));
    }

    @Override
    public Optional<PartnerSummary> findById(PartnerId id) {
        return repo.findById(id.value()).map(DefaultPartnerApi::toSummary);
    }

    @Override
    public Map<PartnerId, PartnerSummary> findAllByIds(Collection<PartnerId> ids) {
        Set<String> rawIds = new HashSet<>();
        for (PartnerId id : ids) {
            rawIds.add(id.value());
        }
        Map<PartnerId, PartnerSummary> result = new HashMap<>();
        for (Partner p : repo.findAllById(rawIds)) {
            var summary = toSummary(p);
            result.put(summary.id(), summary);
        }
        return result;
    }

    @Override
    public boolean exists(PartnerId id) {
        return repo.existsById(id.value());
    }

    @Override
    public Page<PartnerSummary> search(
            @Nullable String q,
            @Nullable PartnerKind kind,
            @Nullable Boolean disabled,
            Pageable pageable) {
        String kindFilter = kind == null ? null : kind.name();
        return repo.searchNative(q, kindFilter, disabled, pageable)
                .map(DefaultPartnerApi::toSummary);
    }

    private static PartnerSummary toSummary(Partner p) {
        return new PartnerSummary(
                p.id(),
                p.code(),
                p.name(),
                p.kind(),
                p.responsibleEmail(),
                p.responsibleContact(),
                p.bRate(),
                p.isDisabled());
    }
}
