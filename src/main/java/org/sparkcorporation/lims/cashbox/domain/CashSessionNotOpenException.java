package org.sparkcorporation.lims.cashbox.domain;

import java.time.LocalDate;

public class CashSessionNotOpenException extends RuntimeException {

    public CashSessionNotOpenException(String cashRegisterId, LocalDate sessionDate) {
        super(
                "Cash session is not OPEN: cashRegisterId="
                        + cashRegisterId
                        + ", sessionDate="
                        + sessionDate);
    }
}
