package org.sparkcorporation.lims.result.web;

/**
 * §V {@code patientType} query-param. CDC §V line 154 lists 4 categories — {@link #INSURED} and
 * {@link #NOT_INSURED} key off {@code patients.insurance_id}; {@link #SOCIETY} and {@link
 * #PARTNER_LAB} key off {@code partners.kind} via {@code patients.partner_id}. All four are wired
 * end-to-end as of 1.6.a billing.
 */
public enum PatientType {
    INSURED,
    NOT_INSURED,
    SOCIETY,
    PARTNER_LAB
}
