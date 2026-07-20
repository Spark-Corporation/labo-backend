package org.sparkcorporation.lims.cashbox.domain;

import java.time.LocalDate;

public class CashSessionNotClosedException extends RuntimeException {

    public CashSessionNotClosedException(String cashRegisterId, LocalDate sessionDate) {
        super(
                "Cash session is not CLOSED: cashRegisterId="
                        + cashRegisterId
                        + ", sessionDate="
                        + sessionDate);
    }
}
