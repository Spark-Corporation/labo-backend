package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PUT /api/v1/catalog/analyses/{id}/techniques}.
 *
 * <p>Full-replace semantics: items list replaces the entire technique link set. At most one item
 * may have {@code isDefault = true} (validated at the service layer; rejected with {@code
 * MultipleDefaultTechniquesException} → 422).
 */
public record PutTechniquesRequest(@NotNull @Valid List<@Valid TechniqueLinkPayload> items) {

    public record TechniqueLinkPayload(
            @NotNull @Size(max = 26) String techniqueId, boolean isDefault) {}
}
