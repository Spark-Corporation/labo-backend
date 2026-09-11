package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "A recorded home-collection fee")
public record HomeCollectionFeeResponse(
        @Schema(description = "ULID of this fee record") String id,
        @Schema(description = "ULID of the patient the home collection was performed for")
                String patientId,
        @Schema(description = "ULID of the lab file (dossier) this fee is billed against")
                String labFileId,
        @Schema(description = "Distance travelled for the collection, in kilometers")
                BigDecimal distanceKm,
        @Schema(description = "Fee amount, in XOF") BigDecimal amount,
        @Schema(description = "URI of the linked receipt scan, if any")
                @Nullable String receiptScanUri,
        @Schema(description = "ULID of the cash register the fee was recorded against")
                String cashRegisterId,
        @Schema(description = "User id who recorded the fee") String recordedBy,
        @Schema(description = "Timestamp the fee was recorded") Instant recordedAt) {}
