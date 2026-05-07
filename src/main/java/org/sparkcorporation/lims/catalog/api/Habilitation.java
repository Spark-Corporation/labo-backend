package org.sparkcorporation.lims.catalog.api;

/**
 * Phlebotomy difficulty categories per CDC §5 "Préleveurs".
 *
 * <p>Values are immutable i18n keys; French labels resolved by message bundle. Order is preserved
 * across releases — UIs and reports may iterate {@link #values()} for display.
 */
public enum Habilitation {
    TOUT_LE_MONDE,
    ADULTES_VEINES_VISIBLES,
    ADULTES_VEINES_PROFONDES_OU_FINES,
    PERSONNES_EN_FORME,
    ENFANTS_VEINES_VISIBLES,
    ENFANTS_VEINES_FINES,
    NOURRISSON_NOUVEAU_NE
}
