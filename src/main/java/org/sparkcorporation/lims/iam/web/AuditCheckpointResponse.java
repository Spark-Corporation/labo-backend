package org.sparkcorporation.lims.iam.web;

import java.time.Instant;

import dev.sparksam.securedx.core.signing.AuditCheckpoint;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Read-side projection of a freshly created {@link AuditCheckpoint}. {@code subjectCount} is the
 * number of distinct subject chains pinned by the checkpoint (the size of its chain-head map); the
 * raw signature and per-subject chain heads are not exposed.
 */
@Schema(description = "A newly created, signed audit checkpoint over the current hash-chain heads.")
public record AuditCheckpointResponse(
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
                int subjectCount) {

    public static AuditCheckpointResponse from(AuditCheckpoint c) {
        return new AuditCheckpointResponse(
                c.checkpointId(),
                c.globalSequence(),
                c.timestamp(),
                c.merkleRoot(),
                c.chainHeads().size());
    }
}
