package org.sparkcorporation.lims.order.api;

/**
 * Kind of discount applied to a {@code LabFile} billing block per CDC §II "Section Facturation".
 *
 * <p>Lives in {@code order::api} (not {@code billing::api}) because the persisted column is on
 * {@code lab_files} — the billing module imports it via its allowed dependency on {@code
 * order::api}. {@code AMOUNT} = absolute XOF amount; {@code PERCENT} = fraction in {@code [0, 1]}.
 */
public enum DiscountKind {
    AMOUNT,
    PERCENT
}
