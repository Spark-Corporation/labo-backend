package org.sparkcorporation.lims.labsettings.api;

/** Lab identity read/write API. */
public interface LabSettingsApi {

    /**
     * Returns the lab settings. Never empty in production — the default row is seeded by Liquibase
     * at first deployment and is never deleted.
     *
     * @throws IllegalStateException if the singleton row is missing (administrator must restore via
     *     Liquibase)
     */
    LabSettings get();

    /** Replace all four fields; returns the new state with bumped {@code updatedAt}. */
    LabSettings update(LabSettingsUpdateRequest req);
}
