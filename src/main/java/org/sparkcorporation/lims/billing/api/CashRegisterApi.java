package org.sparkcorporation.lims.billing.api;

import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.sparkcorporation.lims.billing.domain.NewCashRegister;
import org.sparkcorporation.lims.billing.domain.UpdateCashRegister;
import org.sparkcorporation.lims.iam.api.UserId;

/**
 * Cross-module CRUD interface for the {@code CashRegister} aggregate (spec §5.3 + §7.3). Payment
 * (chunk 7) uses {@link #isUserAssigned} to enforce "only the cashier assigned to the till can
 * collect through it" per CDC §VIII.
 */
public interface CashRegisterApi {

    CashRegisterSummary create(NewCashRegister req);

    CashRegisterSummary update(CashRegisterId id, UpdateCashRegister req);

    void disable(CashRegisterId id);

    Optional<CashRegisterSummary> findById(CashRegisterId id);

    Page<CashRegisterSummary> search(
            @Nullable String q, @Nullable Boolean disabled, Pageable pageable);

    boolean existsById(CashRegisterId id);

    boolean isUserAssigned(CashRegisterId registerId, UserId userId);

    /**
     * Replace the assigned-user set wholesale (not append). Validates every user via {@code
     * UserApi.existsById}; partial validation failure rejects the entire call with {@link
     * org.sparkcorporation.lims.billing.domain.CashRegisterInvalidUserReferenceException}.
     */
    void assignUsers(CashRegisterId id, Set<UserId> userIds);
}
