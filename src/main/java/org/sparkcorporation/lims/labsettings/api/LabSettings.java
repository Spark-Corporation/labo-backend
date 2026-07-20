package org.sparkcorporation.lims.labsettings.api;

import java.time.Instant;

/**
 * Lab identity values rendered on documents (CR PDF header / footer; future: receipts, quotes,
 * invoices). Singleton — read-only from non-admin code paths.
 *
 * <p>{@code updatedAt} is exposed on the public DTO so the admin UI can show a "last updated"
 * timestamp. Other consumers (CR PDF) ignore it.
 */
public record LabSettings(
        String name,
        String address,
        String accreditationNumber,
        String managerName,
        Instant updatedAt) {}
