package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.domain.NewInsurance;
import org.sparkcorporation.lims.catalog.domain.UpdateInsurance;

/**
 * Surface-contract test for {@link InsuranceApi}. Verifies that the cross-module return shape
 * carries the fields billing + patient modules rely on (id, code, name, bRate, coverageRate,
 * disabled). Uses an in-memory stub so the assertion is on the API contract, not on JPA wiring (see
 * {@code DefaultInsuranceApiTest} for the persistence-side contract).
 */
class InsuranceApiTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-12T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void create_returns_summary_with_b_rate_and_coverage() {
        NewInsurance req =
                new NewInsurance(
                        "CNSS",
                        "Caisse Nationale de Sécurité Sociale",
                        "contact@cnss.tg",
                        "+228 22 22 22 22",
                        new BigDecimal("1500.00"),
                        new BigDecimal("0.8000"));

        InsuranceApi api = stubApiCreatingOne();
        InsuranceSummary summary = api.create(req);

        assertThat(summary.code()).isEqualTo("CNSS");
        assertThat(summary.name()).isEqualTo("Caisse Nationale de Sécurité Sociale");
        assertThat(summary.bRate()).isEqualByComparingTo("1500.00");
        assertThat(summary.coverageRate()).isEqualByComparingTo("0.8000");
        assertThat(summary.disabled()).isFalse();
    }

    private InsuranceApi stubApiCreatingOne() {
        return new InMemoryInsuranceApi();
    }

    /** In-memory {@link InsuranceApi} — exercises the API contract without JPA. */
    private static final class InMemoryInsuranceApi implements InsuranceApi {

        private final Map<String, InsuranceSummary> byId = new HashMap<>();

        @Override
        public InsuranceSummary create(NewInsurance req) {
            var id = InsuranceId.generate();
            var summary =
                    new InsuranceSummary(
                            id,
                            req.code(),
                            req.name(),
                            req.responsibleEmail(),
                            req.responsibleContact(),
                            req.bRate(),
                            req.coverageRate(),
                            false);
            byId.put(id.value(), summary);
            return summary;
        }

        @Override
        public InsuranceSummary update(InsuranceId id, UpdateInsurance req) {
            throw new UnsupportedOperationException("not exercised by this test");
        }

        @Override
        public void disable(InsuranceId id) {
            throw new UnsupportedOperationException("not exercised by this test");
        }

        @Override
        public java.util.Optional<InsuranceSummary> findById(InsuranceId id) {
            return java.util.Optional.ofNullable(byId.get(id.value()));
        }

        @Override
        public Map<InsuranceId, InsuranceSummary> findAllByIds(
                java.util.Collection<InsuranceId> ids) {
            Map<InsuranceId, InsuranceSummary> out = new HashMap<>();
            for (InsuranceId id : ids) {
                var s = byId.get(id.value());
                if (s != null) out.put(id, s);
            }
            return out;
        }

        @Override
        public boolean exists(InsuranceId id) {
            return byId.containsKey(id.value());
        }

        @Override
        public org.springframework.data.domain.Page<InsuranceSummary> search(
                @org.jspecify.annotations.Nullable String q,
                @org.jspecify.annotations.Nullable Boolean disabled,
                org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("not exercised by this test");
        }

        // Keep FIXED_CLOCK referenced for parity with future tests that introduce time-sensitive
        // assertions; deliberately unused for the create-shape test.
        @SuppressWarnings("unused")
        private Clock clock() {
            return FIXED_CLOCK;
        }
    }
}
