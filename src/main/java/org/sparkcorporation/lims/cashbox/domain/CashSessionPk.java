package org.sparkcorporation.lims.cashbox.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class CashSessionPk implements Serializable {

    private String cashRegisterId;
    private LocalDate sessionDate;

    public CashSessionPk() {}

    public CashSessionPk(String cashRegisterId, LocalDate sessionDate) {
        this.cashRegisterId = cashRegisterId;
        this.sessionDate = sessionDate;
    }

    public String getCashRegisterId() {
        return cashRegisterId;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CashSessionPk p)) {
            return false;
        }
        return Objects.equals(cashRegisterId, p.cashRegisterId)
                && Objects.equals(sessionDate, p.sessionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cashRegisterId, sessionDate);
    }
}
