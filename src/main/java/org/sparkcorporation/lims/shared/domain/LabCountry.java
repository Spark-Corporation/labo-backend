package org.sparkcorporation.lims.shared.domain;

/**
 * Country in which a LIMS instance operates — drives invoice numbering prefix per CDC Gestion
 * comptable §1.
 */
public enum LabCountry {
    TOGO("LTA"),
    BENIN("LBA");

    private final String invoicePrefix;

    LabCountry(String invoicePrefix) {
        this.invoicePrefix = invoicePrefix;
    }

    public String invoicePrefix() {
        return invoicePrefix;
    }
}
