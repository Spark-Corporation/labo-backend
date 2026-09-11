package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "Body for recording a new home-collection fee")
public record HomeCollectionFeeRequest(
        @Schema(description = "ULID of the patient the home collection was performed for") @NotBlank
                String patientId,
        @Schema(
                        description =
                                "ULID of the lab file (dossier) this fee is billed against; the lab"
                                        + " file must be OPEN")
                @NotBlank
                String labFileId,
        @Schema(description = "Distance travelled for the collection, in kilometers; must be >= 0")
                @NotNull
                @DecimalMin(value = "0.00", message = "distanceKm must be >= 0")
                BigDecimal distanceKm,
        @Schema(description = "Fee amount in XOF; must be strictly positive")
                @NotNull
                @DecimalMin(value = "0.01", message = "amount must be > 0")
                BigDecimal amount,
        @Schema(description = "URI of an already-uploaded receipt scan, if any")
                @Nullable String receiptScanUri,
        @Schema(description = "ULID of the cash register the fee is recorded against") @NotBlank
                String cashRegisterId) {}
