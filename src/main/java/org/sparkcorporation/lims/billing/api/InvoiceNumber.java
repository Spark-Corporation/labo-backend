package org.sparkcorporation.lims.billing.api;

import java.util.regex.Pattern;

/**
 * Invoice number value object. Format: {@code <labCode><yy>-<MMdd>-<seq>} where {@code labCode} is
 * a 3-letter uppercase identifier ({@code lims.lab.code}), {@code yy} is the 2-digit year, {@code
 * MMdd} the month + day, and {@code seq} the per-(labCode,date) zero-padded sequence (≥ 3 digits).
 *
 * <p>Example: {@code LTA24-1710-005}. The regex matches the DB CHECK constraint {@code
 * invoices_number_format_chk} exactly.
 */
public record InvoiceNumber(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{3}\\d{2}-\\d{4}-\\d{3,}$");

    public InvoiceNumber {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid invoice number: " + value);
        }
    }
}
