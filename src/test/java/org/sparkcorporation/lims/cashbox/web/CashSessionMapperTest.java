package org.sparkcorporation.lims.cashbox.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.SessionStatus;
import org.sparkcorporation.lims.cashbox.domain.CashSession;
import org.sparkcorporation.lims.iam.api.UserId;

class CashSessionMapperTest {

    @Test
    void mapsClosedSessionAllFields() {
        var register = CashRegisterId.generate();
        var by = UserId.next();
        var clock = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
        var session = CashSession.openIfMissing(register, LocalDate.parse("2026-05-18"));
        session.close(
                new CashSession.Totals(
                        new BigDecimal("100"),
                        new BigDecimal("50"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("20"),
                        new BigDecimal("30")),
                by,
                clock);

        var resp = CashSessionMapper.toResponse(session);

        assertThat(resp.cashRegisterId()).isEqualTo(register.value());
        assertThat(resp.sessionDate()).isEqualTo(LocalDate.parse("2026-05-18"));
        assertThat(resp.status()).isEqualTo(SessionStatus.CLOSED);
        assertThat(resp.cashTotal()).isEqualByComparingTo("100");
        assertThat(resp.chequeTotal()).isEqualByComparingTo("50");
        assertThat(resp.expensesTotal()).isEqualByComparingTo("20");
        assertThat(resp.homeCollectionTotal()).isEqualByComparingTo("30");
        assertThat(resp.grandTotal()).isEqualByComparingTo("160");
        assertThat(resp.closedBy()).isEqualTo(by.value());
        assertThat(resp.closedAt()).isEqualTo(Instant.parse("2026-05-18T10:00:00Z"));
        assertThat(resp.reopenedAt()).isNull();
    }
}
