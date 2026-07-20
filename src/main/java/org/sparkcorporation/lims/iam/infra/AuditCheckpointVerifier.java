package org.sparkcorporation.lims.iam.infra;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-verifies the latest persisted audit checkpoint against the live ledger.
 *
 * <p>It reuses {@link AuditCheckpointService#currentHeads()} and {@link
 * AuditCheckpointService#canonicalPayload} so verification and signing share one source of truth —
 * the recomputed Merkle root and rebuilt canonical payload are byte-identical to what {@code
 * createCheckpoint} produced for the same inputs.
 */
@Service
@RequiredArgsConstructor
public class AuditCheckpointVerifier {

    private final SignedActionRepository signedActions;
    private final AuditCheckpointRepository checkpoints;
    private final AuditCheckpointService checkpointService;
    private final SystemAuditIdentity identity;

    /**
     * Verifies the most recent checkpoint, or {@link Optional#empty()} when none exists (the caller
     * maps that to 404).
     *
     * <ul>
     *   <li>{@code signatureValid} — the system audit key still verifies the canonical payload
     *       rebuilt from the STORED checkpoint fields (proves byte-identity with the signed form);
     *   <li>{@code headsMatch} — the Merkle root recomputed from the CURRENT chain heads still
     *       equals the stored {@code merkleRoot} (i.e. no chain head has moved since);
     *   <li>{@code actionsAppendedSince} — how many signed actions have been appended since the
     *       checkpoint's {@code globalSequence}.
     * </ul>
     */
    @Transactional(readOnly = true)
    public Optional<Result> verifyLatest() {
        return checkpoints.findFirstByOrderByGlobalSequenceDescTimestampDesc().map(this::verify);
    }

    private Result verify(AuditCheckpointEntity stored) {
        Map<String, String> currentHeads = checkpointService.currentHeads();
        String recomputedRoot = AuditMerkle.root(currentHeads);
        boolean headsMatch = recomputedRoot.equals(stored.getMerkleRoot());

        String payload =
                AuditCheckpointService.canonicalPayload(
                        stored.getCheckpointId(),
                        stored.getGlobalSequence(),
                        stored.getTimestamp(),
                        stored.getMerkleRoot(),
                        stored.getChainHeads());
        boolean signatureValid =
                identity.verify(payload.getBytes(StandardCharsets.UTF_8), stored.getSignature());

        long actionsAppendedSince = signedActions.count() - stored.getGlobalSequence();

        return new Result(
                stored.getCheckpointId(),
                stored.getGlobalSequence(),
                stored.getTimestamp(),
                stored.getMerkleRoot(),
                stored.getChainHeads().size(),
                signatureValid,
                headsMatch,
                actionsAppendedSince);
    }

    /** Verification outcome for the latest checkpoint, plus a summary of the checkpoint itself. */
    public record Result(
            String checkpointId,
            long globalSequence,
            Instant timestamp,
            String merkleRoot,
            int subjectCount,
            boolean signatureValid,
            boolean headsMatch,
            long actionsAppendedSince) {}
}
