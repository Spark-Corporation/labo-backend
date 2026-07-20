package org.sparkcorporation.lims.billing.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.sparkcorporation.lims.billing.domain.NewCashRegister;
import org.sparkcorporation.lims.billing.domain.UpdateCashRegister;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Surface-contract test for {@link CashRegisterApi}. Uses an in-memory stub so the test pins the
 * API shape — specifically the replace-not-append semantics of {@link CashRegisterApi#assignUsers}
 * — without coupling to JPA wiring. The behavioral cover at the persistence layer lives in {@code
 * CashRegisterIT}.
 */
class CashRegisterApiTest {

    @Test
    void assign_users_replaces_membership_set() {
        // ARRANGE: a cash register with users {A, B} assigned.
        CashRegisterApi api = new InMemoryCashRegisterApi();
        var summary = api.create(new NewCashRegister("C1", "Caisse 1", 1));
        var regId = summary.id();
        var userA = UserId.next();
        var userB = UserId.next();
        var userC = UserId.next();
        api.assignUsers(regId, Set.of(userA, userB));

        // ACT: replace assignment with {B, C}.
        api.assignUsers(regId, Set.of(userB, userC));

        // ASSERT: register now has exactly {B, C} — not {A, B, C}.
        var reloaded = api.findById(regId).orElseThrow();
        assertThat(reloaded.userIds())
                .containsExactlyInAnyOrder(userB.value(), userC.value())
                .doesNotContain(userA.value());
    }

    /** In-memory stub: exercises the API contract without JPA. */
    private static final class InMemoryCashRegisterApi implements CashRegisterApi {

        private final Map<String, CashRegisterSummary> byId = new HashMap<>();

        @Override
        public CashRegisterSummary create(NewCashRegister req) {
            var id = CashRegisterId.of(Ulid.generate());
            var summary =
                    new CashRegisterSummary(
                            id,
                            req.code(),
                            req.label(),
                            req.sequenceNumber(),
                            Set.of(),
                            false,
                            Instant.now());
            byId.put(id.value(), summary);
            return summary;
        }

        @Override
        public CashRegisterSummary update(CashRegisterId id, UpdateCashRegister req) {
            throw new UnsupportedOperationException("not exercised by this test");
        }

        @Override
        public void disable(CashRegisterId id) {
            throw new UnsupportedOperationException("not exercised by this test");
        }

        @Override
        public Optional<CashRegisterSummary> findById(CashRegisterId id) {
            return Optional.ofNullable(byId.get(id.value()));
        }

        @Override
        public Page<CashRegisterSummary> search(
                @org.jspecify.annotations.Nullable String q,
                @org.jspecify.annotations.Nullable Boolean disabled,
                Pageable pageable) {
            return new PageImpl<>(java.util.List.copyOf(byId.values()));
        }

        @Override
        public boolean existsById(CashRegisterId id) {
            return byId.containsKey(id.value());
        }

        @Override
        public boolean isUserAssigned(CashRegisterId registerId, UserId userId) {
            var s = byId.get(registerId.value());
            return s != null && s.userIds().contains(userId.value());
        }

        @Override
        public void assignUsers(CashRegisterId id, Set<UserId> userIds) {
            var existing = byId.get(id.value());
            if (existing == null) {
                throw new IllegalArgumentException("not found: " + id.value());
            }
            Set<String> newUserIds =
                    new HashSet<>(userIds.stream().map(UserId::value).collect(Collectors.toSet()));
            byId.put(
                    id.value(),
                    new CashRegisterSummary(
                            existing.id(),
                            existing.code(),
                            existing.label(),
                            existing.sequenceNumber(),
                            newUserIds,
                            existing.disabled(),
                            existing.createdAt()));
        }
    }
}
