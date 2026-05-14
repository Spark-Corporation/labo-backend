package org.sparkcorporation.lims.billing.infra;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterCreated;
import org.sparkcorporation.lims.billing.api.CashRegisterDisabled;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.CashRegisterSummary;
import org.sparkcorporation.lims.billing.api.CashRegisterUpdated;
import org.sparkcorporation.lims.billing.domain.CashRegister;
import org.sparkcorporation.lims.billing.domain.CashRegisterInvalidUserReferenceException;
import org.sparkcorporation.lims.billing.domain.CashRegisterNotFoundException;
import org.sparkcorporation.lims.billing.domain.NewCashRegister;
import org.sparkcorporation.lims.billing.domain.UpdateCashRegister;
import org.sparkcorporation.lims.iam.api.UserApi;
import org.sparkcorporation.lims.iam.api.UserId;

/**
 * Default {@link CashRegisterApi} implementation (spec §7.3). Validates cross-module user
 * references through {@link UserApi#existsById}; the modulith boundary on {@code billing →
 * iam::api} is exercised at runtime here.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultCashRegisterApi implements CashRegisterApi {

    private final CashRegisterRepository repo;
    private final UserApi userApi;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public CashRegisterSummary create(NewCashRegister req) {
        var register = CashRegister.create(req, clock);
        repo.save(register);
        events.publishEvent(
                new CashRegisterCreated(
                        register.cashRegisterId(), register.getCode(), Instant.now(clock)));
        return toSummary(register);
    }

    @Override
    @Transactional
    public CashRegisterSummary update(CashRegisterId id, UpdateCashRegister req) {
        var register =
                repo.findById(id.value()).orElseThrow(() -> new CashRegisterNotFoundException(id));
        register.update(req, clock);
        events.publishEvent(new CashRegisterUpdated(id, Instant.now(clock)));
        return toSummary(register);
    }

    @Override
    @Transactional
    public void disable(CashRegisterId id) {
        var register = repo.findById(id.value()).orElse(null);
        if (register == null) {
            // Idempotent: already disabled (hidden by @SQLRestriction) or never existed.
            return;
        }
        register.disable(clock);
        events.publishEvent(new CashRegisterDisabled(id, Instant.now(clock)));
    }

    @Override
    public Optional<CashRegisterSummary> findById(CashRegisterId id) {
        return repo.findById(id.value()).map(DefaultCashRegisterApi::toSummary);
    }

    @Override
    public Page<CashRegisterSummary> search(
            @Nullable String q, @Nullable Boolean disabled, Pageable pageable) {
        return repo.searchNative(q, disabled, pageable).map(DefaultCashRegisterApi::toSummary);
    }

    @Override
    public boolean existsById(CashRegisterId id) {
        return repo.existsById(id.value());
    }

    @Override
    public boolean isUserAssigned(CashRegisterId registerId, UserId userId) {
        return repo.isUserAssigned(registerId.value(), userId.value());
    }

    @Override
    @Transactional
    public void assignUsers(CashRegisterId id, Set<UserId> userIds) {
        var register =
                repo.findById(id.value()).orElseThrow(() -> new CashRegisterNotFoundException(id));

        // Validate ALL users up front — partial assignment is not allowed.
        for (UserId uid : userIds) {
            if (!userApi.existsById(uid)) {
                throw new CashRegisterInvalidUserReferenceException(uid);
            }
        }

        register.replaceAssignments(new LinkedHashSet<>(userIds), clock);
        events.publishEvent(new CashRegisterUpdated(id, Instant.now(clock)));
    }

    static CashRegisterSummary toSummary(CashRegister r) {
        return new CashRegisterSummary(
                r.cashRegisterId(),
                r.getCode(),
                r.getLabel(),
                r.getSequenceNumber(),
                r.userIds(),
                r.isDisabled(),
                r.getCreatedAt());
    }
}
