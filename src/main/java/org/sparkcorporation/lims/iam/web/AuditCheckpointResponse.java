package org.sparkcorporation.lims.iam.web;

import java.time.Instant;

import dev.sparksam.securedx.core.signing.AuditCheckpoint;

/**
 * Read-side projection of a freshly created {@link AuditCheckpoint}. {@code subjectCount} is the
 * number of distinct subject chains pinned by the checkpoint (the size of its chain-head map); the
 * raw signature and per-subject chain heads are not exposed.
 */
public record AuditCheckpointResponse(
        String checkpointId,
        long globalSequence,
        Instant timestamp,
        String merkleRoot,
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
