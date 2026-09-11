package org.sparkcorporation.lims.iam.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.iam.infra.AuditCheckpointVerifier;

/**
 * Read-side projection of {@link AuditCheckpointVerifier.Result} for {@code GET
 * /api/v1/audit/checkpoints/latest}: a summary of the latest checkpoint plus its live verification
 * outcome. The raw signature and per-subject chain heads are not exposed.
 */
@Schema(
        description =
                "Summary of the latest audit checkpoint plus its live re-verification outcome.")
public record CheckpointVerifyResponse(
        @Schema(description = "Unique identifier of the checkpoint.") String checkpointId,
        @Schema(description = "Total number of signed actions in the ledger at checkpoint time.")
                long globalSequence,
        @Schema(description = "Instant at which the checkpoint was created and signed.")
                Instant timestamp,
        @Schema(
                        description =
                                "Merkle root computed over every subject's chain head at checkpoint"
                                        + " time.")
                String merkleRoot,
        @Schema(description = "Number of distinct subject hash-chains pinned by this checkpoint.")
                int subjectCount,
        @Schema(
                        description =
                                "True if the system audit key still verifies the canonical payload"
                                        + " rebuilt from the stored checkpoint fields, proving"
                                        + " byte-identity with the signed form.")
                boolean signatureValid,
        @Schema(
                        description =
                                "True if the Merkle root recomputed from the current chain heads"
                                    + " still equals the stored merkleRoot, i.e. no chain head has"
                                    + " moved since the checkpoint was taken.")
                boolean headsMatch,
        @Schema(
                        description =
                                "Number of signed actions appended to the ledger since this"
                                        + " checkpoint.")
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
