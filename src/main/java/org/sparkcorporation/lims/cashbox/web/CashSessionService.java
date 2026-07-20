package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.domain.CashSession;
import org.sparkcorporation.lims.cashbox.domain.CashSessionNotClosedException;
import org.sparkcorporation.lims.cashbox.infra.CashSessionRepository;
import org.sparkcorporation.lims.iam.api.UserId;

@Service
@RequiredArgsConstructor
public class CashSessionService {

    private final CashSessionRepository sessionRepo;
    private final CashRegisterApi cashRegisterApi;
    private final Clock clock;

    @Transactional
    public CashSessionResponse close(CashRegisterId registerId, LocalDate date, UserId by) {
        if (!cashRegisterApi.existsById(registerId)) {
            throw new IllegalArgumentException(
                    "cashRegisterId does not exist: " + registerId.value());
        }
        var session =
                sessionRepo
                        .findByCashRegisterIdAndSessionDate(registerId.value(), date)
                        .orElseGet(
                                () ->
                                        sessionRepo.save(
                                                CashSession.openIfMissing(registerId, date)));
        var t = sessionRepo.computeTotals(registerId.value(), date);
        session.close(
                new CashSession.Totals(
                        nullSafe(t.getCash()),
                        nullSafe(t.getCheque()),
                        nullSafe(t.getCardTpe()),
                        nullSafe(t.getFlooz()),
                        nullSafe(t.getMixxByYas()),
                        nullSafe(t.getExpenses()),
                        nullSafe(t.getHomeCollection())),
                by,
                clock);
        return CashSessionMapper.toResponse(sessionRepo.save(session));
    }

    @Transactional
    public CashSessionResponse reopen(
            CashRegisterId registerId, LocalDate date, String reason, UserId by) {
        var session =
                sessionRepo
                        .findByCashRegisterIdAndSessionDate(registerId.value(), date)
                        .orElseThrow(
                                () -> new CashSessionNotClosedException(registerId.value(), date));
        session.reopen(reason, by, clock);
        return CashSessionMapper.toResponse(sessionRepo.save(session));
    }

    @Transactional(readOnly = true)
    public CashSessionResponse get(CashRegisterId registerId, LocalDate date) {
        return sessionRepo
                .findByCashRegisterIdAndSessionDate(registerId.value(), date)
                .map(CashSessionMapper::toResponse)
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "No session for register="
                                                + registerId.value()
                                                + " date="
                                                + date));
    }

    private static BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
