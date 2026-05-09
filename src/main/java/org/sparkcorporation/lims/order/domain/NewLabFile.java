package org.sparkcorporation.lims.order.domain;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Command record passed to {@link LabFile#open(NewLabFile,
 * org.sparkcorporation.lims.order.api.LabFileRegistrationNumber, String, java.time.Clock)}. The
 * compact constructor copies {@code analysisCodes} via {@link List#copyOf(java.util.Collection)} so
 * the aggregate is insulated from caller-side mutations.
 */
public record NewLabFile(
        String patientId,
        @Nullable String prescriberId,
        @Nullable String preleveurId,
        boolean urgent,
        List<String> analysisCodes) {

    public NewLabFile {
        analysisCodes = List.copyOf(analysisCodes);
    }
}
