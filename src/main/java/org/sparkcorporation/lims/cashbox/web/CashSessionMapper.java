package org.sparkcorporation.lims.cashbox.web;

import org.sparkcorporation.lims.cashbox.domain.CashSession;

public final class CashSessionMapper {

    private CashSessionMapper() {}

    public static CashSessionResponse toResponse(CashSession session) {
        return new CashSessionResponse(
                session.getCashRegisterId(),
                session.getSessionDate(),
                session.getStatus(),
                session.getCashTotal(),
                session.getChequeTotal(),
                session.getCardTpeTotal(),
                session.getFloozTotal(),
                session.getMixxByYasTotal(),
                session.getExpensesTotal(),
                session.getHomeCollectionTotal(),
                session.getGrandTotal(),
                session.getClosedBy(),
                session.getClosedAt(),
                session.getReopenedBy(),
                session.getReopenedAt(),
                session.getReopenReason());
    }
}
