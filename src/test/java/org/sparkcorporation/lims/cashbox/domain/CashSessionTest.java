package org.sparkcorporation.lims.cashbox.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.SessionStatus;
import org.sparkcorporation.lims.iam.api.UserId;

class CashSessionTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
    private static final CashRegisterId REGISTER = CashRegisterId.generate();
    private static final LocalDate DATE = LocalDate.parse("2026-05-18");
    private static final UserId BY = UserId.next();

    @Test
    void openSessionIsTheDefaultStateOnCreate() {
        var session = CashSession.openIfMissing(REGISTER, DATE);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.OPEN);
        assertThat(session.getCashRegisterId()).isEqualTo(REGISTER.value());
        assertThat(session.getSessionDate()).isEqualTo(DATE);
        assertThat(session.getCashTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(session.getChequeTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(session.getCardTpeTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(session.getFloozTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(session.getMixxByYasTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(session.getExpensesTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(session.getHomeCollectionTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(session.getGrandTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(session.getClosedAt()).isNull();
        assertThat(session.getClosedBy()).isNull();
    }

    @Test
    void closeFromOpenSetsClosedStatusAndTotals() {
        var session = CashSession.openIfMissing(REGISTER, DATE);
        var totals =
                new CashSession.Totals(
                        new BigDecimal("10000"),
                        new BigDecimal("5000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("2000"),
                        new BigDecimal("3000"));

        session.close(totals, BY, FIXED);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(session.getCashTotal()).isEqualByComparingTo("10000");
        assertThat(session.getChequeTotal()).isEqualByComparingTo("5000");
        assertThat(session.getExpensesTotal()).isEqualByComparingTo("2000");
        assertThat(session.getHomeCollectionTotal()).isEqualByComparingTo("3000");
        assertThat(session.getGrandTotal()).isEqualByComparingTo("16000");
        assertThat(session.getClosedBy()).isEqualTo(BY.value());
        assertThat(session.getClosedAt()).isEqualTo(Instant.parse("2026-05-18T10:00:00Z"));
    }

    @Test
    void closeFromClosedThrows() {
        var session = CashSession.openIfMissing(REGISTER, DATE);
        var zero =
                new CashSession.Totals(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        session.close(zero, BY, FIXED);

        assertThatThrownBy(() -> session.close(zero, BY, FIXED))
                .isInstanceOf(CashSessionNotOpenException.class);
    }

    @Test
    void reopenFromClosedFlipsStatusAndCapturesReason() {
        var session = CashSession.openIfMissing(REGISTER, DATE);
        var zero =
                new CashSession.Totals(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        session.close(zero, BY, FIXED);

        var reopener = UserId.next();
        session.reopen("typo fix", reopener, FIXED);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.OPEN);
        assertThat(session.getReopenedBy()).isEqualTo(reopener.value());
        assertThat(session.getReopenedAt()).isEqualTo(Instant.parse("2026-05-18T10:00:00Z"));
        assertThat(session.getReopenReason()).isEqualTo("typo fix");
    }

    @Test
    void reopenFromOpenThrows() {
        var session = CashSession.openIfMissing(REGISTER, DATE);
        assertThatThrownBy(() -> session.reopen("typo", BY, FIXED))
                .isInstanceOf(CashSessionNotClosedException.class);
    }

    @Test
    void reopenWithBlankReasonThrows() {
        var session = CashSession.openIfMissing(REGISTER, DATE);
        var zero =
                new CashSession.Totals(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        session.close(zero, BY, FIXED);

        assertThatThrownBy(() -> session.reopen("   ", BY, FIXED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
