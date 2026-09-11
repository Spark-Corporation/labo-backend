package org.sparkcorporation.lims.iam.web;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.iam.infra.SignedActionEntity;

/**
 * List-view projection of a {@code signed_actions} row (the persisted mirror of secured-x's {@code
 * SignedAction}). Deliberately omits the raw {@code signature} and {@code previousHash}: callers
 * verifying integrity use {@code POST /api/v1/audit/verify}, which re-checks the cryptographic
 * chain server-side; the read endpoint exposes only the human-auditable fields.
 */
@Schema(description = "One entry of the signed-action audit ledger (human-auditable fields only).")
public record AuditActionResponse(
        @Schema(description = "Unique identifier of this signed action entry.") String actionId,
        @Schema(description = "ULID of the user who performed the action.") String subject,
        @Schema(description = "Name of the recorded action.") String action,
        @Schema(description = "Resource URI the action was performed against, when applicable.")
                @Nullable String resource,
        @Schema(description = "Instant at which the action was performed and signed.")
                Instant timestamp,
        @Schema(
                        description =
                                "Correlation id linking this action to the originating request,"
                                        + " when available.")
                @Nullable String correlationId,
        @Schema(description = "This action's position in the global hash-chain sequence.")
                long sequenceNumber,
        @Schema(description = "Free-form contextual metadata captured with the action, when any.")
                @Nullable Map<String, Object> metadata) {

    static AuditActionResponse from(SignedActionEntity e) {
        return new AuditActionResponse(
                e.getActionId(),
                e.getSubject(),
                e.getAction(),
                e.getResource(),
                e.getTimestamp(),
                e.getCorrelationId(),
                e.getSequenceNumber(),
                e.getMetadata());
    }
}
