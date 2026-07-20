package org.sparkcorporation.lims.cashbox.infra;

import java.math.BigDecimal;

public interface CashSessionTotalsProjection {

    BigDecimal getCash();

    BigDecimal getCheque();

    BigDecimal getCardTpe();

    BigDecimal getFlooz();

    BigDecimal getMixxByYas();

    BigDecimal getExpenses();

    BigDecimal getHomeCollection();
}
