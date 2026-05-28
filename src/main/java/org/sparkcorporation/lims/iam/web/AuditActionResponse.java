package org.sparkcorporation.lims.iam.web;

import java.time.Instant;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.iam.infra.SignedActionEntity;

/**
 * List-view projection of a {@code signed_actions} row (the persisted mirror of secured-x's {@code
 * SignedAction}). Deliberately omits the raw {@code signature} and {@code previousHash}: callers
 * verifying integrity use {@code POST /api/v1/audit/verify}, which re-checks the cryptographic chain
 * server-side; the read endpoint exposes only the human-auditable fields.
 */
public record AuditActionResponse(
        String actionId,
        String subject,
        String action,
        @Nullable String resource,
        Instant timestamp,
        @Nullable String correlationId,
        long sequenceNumber,
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
