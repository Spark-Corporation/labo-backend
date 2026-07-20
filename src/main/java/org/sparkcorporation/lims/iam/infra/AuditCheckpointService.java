package org.sparkcorporation.lims.iam.infra;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import dev.sparksam.securedx.core.signing.AuditCheckpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds a real audit checkpoint (the pinned secured-x manager's createCheckpoint() is a stub):
 * snapshots each subject's chain head, computes a Merkle root, signs the canonical payload with the
 * {@link SystemAuditIdentity} key, and persists it.
 */
@Service
@RequiredArgsConstructor
public class AuditCheckpointService {

    private final SignedActionRepository signedActions;
    private final AuditCheckpointRepository checkpoints;
    private final SystemAuditIdentity identity;
    private final Clock clock;

    @Transactional
    public AuditCheckpoint createCheckpoint() {
        var heads = currentHeads();

        String checkpointId = UUID.randomUUID().toString();
        long globalSequence = signedActions.count();
        Instant timestamp = Instant.now(clock);
        String merkleRoot = AuditMerkle.root(heads);
        String payload =
                canonicalPayload(checkpointId, globalSequence, timestamp, merkleRoot, heads);
        String signature = identity.sign(payload.getBytes(StandardCharsets.UTF_8));

        var e = new AuditCheckpointEntity();
        e.setCheckpointId(checkpointId);
        e.setGlobalSequence(globalSequence);
        e.setTimestamp(timestamp);
        e.setChainHeads(heads);
        e.setMerkleRoot(merkleRoot);
        e.setSignature(signature);
        e.setKeyId(identity.keyId());
        checkpoints.save(e);

        return new AuditCheckpoint(
                checkpointId, globalSequence, timestamp, heads, merkleRoot, signature);
    }

    /**
     * Snapshots every non-system subject's current chain head (the SHA-256 of its latest link's
     * signature), keyed by subject in deterministic ({@link TreeMap} sorted) order. Shared by
     * {@link #createCheckpoint()} and {@link AuditCheckpointVerifier} so the recomputed Merkle root
     * on verify can never diverge from what was signed.
     */
    Map<String, String> currentHeads() {
        var heads = new TreeMap<String, String>();
        for (String subject : signedActions.findDistinctSubjects()) {
            if (SystemAuditIdentity.SUBJECT.equals(subject)) {
                continue;
            }
            signedActions
                    .findFirstBySubjectOrderBySequenceNumberDesc(subject)
                    .ifPresent(
                            head -> heads.put(subject, AuditMerkle.sha256Hex(head.getSignature())));
        }
        return heads;
    }

    /**
     * The exact byte-string that {@link SystemAuditIdentity#sign} signs for a checkpoint: {@code
     * checkpointId|globalSequence|timestamp|merkleRoot|canonicalJson(heads)}. Shared by {@link
     * #createCheckpoint()} and {@link AuditCheckpointVerifier} so verification rebuilds an
     * identical payload from the stored fields — any drift here would break signature verification.
     */
    static String canonicalPayload(
            String checkpointId,
            long globalSequence,
            Instant timestamp,
            String merkleRoot,
            Map<String, String> heads) {
        return checkpointId
                + "|"
                + globalSequence
                + "|"
                + timestamp
                + "|"
                + merkleRoot
                + "|"
                + AuditMerkle.canonicalJson(heads);
    }
}
