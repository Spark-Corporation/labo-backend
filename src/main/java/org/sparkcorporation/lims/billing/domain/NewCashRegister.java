package org.sparkcorporation.lims.billing.domain;

/**
 * Command to create a new {@link CashRegister}. Spec §5.3:
 *
 * <ul>
 *   <li>{@code code} is 2 uppercase alphanumeric chars (unique, immutable after create).
 *   <li>{@code label} is the free-form display label.
 *   <li>{@code sequenceNumber} is the per-cash-register ordering hint (unique).
 * </ul>
 */
public record NewCashRegister(String code, String label, Integer sequenceNumber) {}
