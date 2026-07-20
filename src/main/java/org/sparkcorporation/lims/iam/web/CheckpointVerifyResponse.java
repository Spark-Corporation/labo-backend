package org.sparkcorporation.lims.iam.web;

import java.time.Instant;

import org.sparkcorporation.lims.iam.infra.AuditCheckpointVerifier;

/**
 * Read-side projection of {@link AuditCheckpointVerifier.Result} for {@code GET
 * /api/v1/audit/checkpoints/latest}: a summary of the latest checkpoint plus its live verification
 * outcome. The raw signature and per-subject chain heads are not exposed.
 */
public record CheckpointVerifyResponse(
        String checkpointId,
        long globalSequence,
        Instant timestamp,
        String merkleRoot,
        int subjectCount,
        boolean signatureValid,
        boolean headsMatch,
        long actionsAppendedSince) {

    public static CheckpointVerifyResponse from(AuditCheckpointVerifier.Result r) {
        return new CheckpointVerifyResponse(
                r.checkpointId(),
                r.globalSequence(),
                r.timestamp(),
                r.merkleRoot(),
                r.subjectCount(),
                r.signatureValid(),
                r.headsMatch(),
                r.actionsAppendedSince());
    }
}
