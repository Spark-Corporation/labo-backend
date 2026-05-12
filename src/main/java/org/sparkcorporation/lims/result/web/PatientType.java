package org.sparkcorporation.lims.result.web;

/**
 * §V {@code patientType} query-param. CDC §V line 154 lists 4 categories; 1.5.b ships only the 2
 * computable from current data — {@code société} and {@code laboratoire-partenaire} defer to 1.6
 * billing (spec §2.2).
 */
public enum PatientType {
    INSURED,
    NOT_INSURED
}
