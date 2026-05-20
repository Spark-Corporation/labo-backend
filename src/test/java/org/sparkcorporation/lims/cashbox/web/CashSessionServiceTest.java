package org.sparkcorporation.lims.cashbox.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.SessionStatus;
import org.sparkcorporation.lims.cashbox.domain.CashSession;
import org.sparkcorporation.lims.cashbox.domain.CashSessionNotClosedException;
import org.sparkcorporation.lims.cashbox.domain.CashSessionNotOpenException;
import org.sparkcorporation.lims.cashbox.infra.CashSessionRepository;
import org.sparkcorporation.lims.cashbox.infra.CashSessionTotalsProjection;
import org.sparkcorporation.lims.iam.api.UserId;

@ExtendWith(MockitoExtension.class)
class CashSessionServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-18T18:00:00Z"), ZoneOffset.UTC);
    private static final CashRegisterId REGISTER = CashRegisterId.generate();
    private static final LocalDate DATE = LocalDate.parse("2026-05-18");
    private static final UserId BY = UserId.next();

    @Mock private CashSessionRepository sessionRepo;
    @Mock private CashRegisterApi cashRegisterApi;

    private CashSessionService service;

    @BeforeEach
    void setUp() {
        service = new CashSessionService(sessionRepo, cashRegisterApi, FIXED);
    }

    @Test
    void closeRecomputesNetGrandTotal() {
        when(cashRegisterApi.existsById(REGISTER)).thenReturn(true);
        when(sessionRepo.findByCashRegisterIdAndSessionDate(REGISTER.value(), DATE))
                .thenReturn(Optional.of(CashSession.openIfMissing(REGISTER, DATE)));
        when(sessionRepo.computeTotals(REGISTER.value(), DATE))
                .thenReturn(mockTotals("10000", "5000", "0", "0", "0", "2000", "3000"));
        when(sessionRepo.save(any(CashSession.class))).thenAnswer(a -> a.getArgument(0));

        var response = service.close(REGISTER, DATE, BY);

        assertThat(response.grandTotal()).isEqualByComparingTo("16000");
        assertThat(response.status()).isEqualTo(SessionStatus.CLOSED);
        assertThat(response.cashTotal()).isEqualByComparingTo("10000");
        assertThat(response.chequeTotal()).isEqualByComparingTo("5000");
        assertThat(response.expensesTotal()).isEqualByComparingTo("2000");
        assertThat(response.homeCollectionTotal()).isEqualByComparingTo("3000");
    }

    @Test
    void closeWhenAlreadyClosedThrows() {
        var existing = CashSession.openIfMissing(REGISTER, DATE);
        existing.close(zero(), BY, FIXED);
        when(cashRegisterApi.existsById(REGISTER)).thenReturn(true);
        when(sessionRepo.findByCashRegisterIdAndSessionDate(REGISTER.value(), DATE))
                .thenReturn(Optional.of(existing));
        when(sessionRepo.computeTotals(REGISTER.value(), DATE))
                .thenReturn(mockTotals("0", "0", "0", "0", "0", "0", "0"));

        assertThatThrownBy(() -> service.close(REGISTER, DATE, BY))
                .isInstanceOf(CashSessionNotOpenException.class);
    }

    @Test
    void closeRejectsUnknownRegister() {
        when(cashRegisterApi.existsById(REGISTER)).thenReturn(false);

        assertThatThrownBy(() -> service.close(REGISTER, DATE, BY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reopenHappyPathFlipsStatus() {
        var existing = CashSession.openIfMissing(REGISTER, DATE);
        existing.close(zero(), BY, FIXED);
        when(sessionRepo.findByCashRegisterIdAndSessionDate(REGISTER.value(), DATE))
                .thenReturn(Optional.of(existing));
        when(sessionRepo.save(any(CashSession.class))).thenAnswer(a -> a.getArgument(0));

        var resp = service.reopen(REGISTER, DATE, "typo", BY);

        assertThat(resp.status()).isEqualTo(SessionStatus.OPEN);
        assertThat(resp.reopenReason()).isEqualTo("typo");
        assertThat(resp.reopenedBy()).isEqualTo(BY.value());
    }

    @Test
    void reopenWithBlankReasonThrows() {
        var existing = CashSession.openIfMissing(REGISTER, DATE);
        existing.close(zero(), BY, FIXED);
        when(sessionRepo.findByCashRegisterIdAndSessionDate(REGISTER.value(), DATE))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.reopen(REGISTER, DATE, "  ", BY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reopenWhenOpenThrows() {
        var existing = CashSession.openIfMissing(REGISTER, DATE);
        when(sessionRepo.findByCashRegisterIdAndSessionDate(REGISTER.value(), DATE))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.reopen(REGISTER, DATE, "typo", BY))
                .isInstanceOf(CashSessionNotClosedException.class);
    }

    @Test
    void getReturns404WhenAbsent() {
        when(sessionRepo.findByCashRegisterIdAndSessionDate(REGISTER.value(), DATE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(REGISTER, DATE))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private static CashSession.Totals zero() {
        return new CashSession.Totals(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);
    }

    private static CashSessionTotalsProjection mockTotals(
            String cash,
            String cheque,
            String card,
            String flooz,
            String mixx,
            String expenses,
            String hcf) {
        return new CashSessionTotalsProjection() {
            @Override
            public BigDecimal getCash() {
                return new BigDecimal(cash);
            }

            @Override
            public BigDecimal getCheque() {
                return new BigDecimal(cheque);
            }

            @Override
            public BigDecimal getCardTpe() {
                return new BigDecimal(card);
            }

            @Override
            public BigDecimal getFlooz() {
                return new BigDecimal(flooz);
            }

            @Override
            public BigDecimal getMixxByYas() {
                return new BigDecimal(mixx);
            }

            @Override
            public BigDecimal getExpenses() {
                return new BigDecimal(expenses);
            }

            @Override
            public BigDecimal getHomeCollection() {
                return new BigDecimal(hcf);
            }
        };
    }
}
