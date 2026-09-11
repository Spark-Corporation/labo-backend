package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for {@code PUT /api/v1/catalog/analyses/{id}/techniques}.
 *
 * <p>Full-replace semantics: items list replaces the entire technique link set. At most one item
 * may have {@code isDefault = true} (validated at the service layer; rejected with {@code
 * MultipleDefaultTechniquesException} → 422).
 */
@Schema(description = "Request to fully replace the technique links of an analysis.")
public record PutTechniquesRequest(
        @Schema(
                        description =
                                "Technique links to persist; replaces the entire existing set for"
                                        + " the analysis. At most one entry may be marked default.")
                @NotNull
                @Valid
                List<@Valid TechniqueLinkPayload> items) {

    @Schema(description = "A single technique link on an analysis.")
    public record TechniqueLinkPayload(
            @Schema(description = "ULID of the technique to link.") @NotNull @Size(max = 26)
                    String techniqueId,
            @Schema(
                            description =
                                    "Whether this technique is the default one for the analysis; at"
                                            + " most one link may be default.")
                    boolean isDefault) {}
}
