package org.sparkcorporation.lims.patient.api;

import java.util.Optional;

/** Portal credential verification. The password hash never leaves the {@code patient} module. */
public interface PatientPortalApi {

    /**
     * @return the patient id if {@code portalLogin} exists and {@code rawPassword} matches the
     *     stored hash; {@link Optional#empty()} otherwise (indistinguishable for unknown login vs.
     *     bad password, to avoid account enumeration).
     */
    Optional<PatientId> authenticate(String portalLogin, String rawPassword);
}
