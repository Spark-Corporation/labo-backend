package org.sparkcorporation.lims.iam.infra;

import java.util.List;
import java.util.Optional;

import dev.sparksam.securedx.core.signing.AuditChainStore;
import dev.sparksam.securedx.core.signing.AuditCheckpoint;
import dev.sparksam.securedx.core.signing.SignedAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LimsAuditChainStore implements AuditChainStore {

    private final SignedActionRepository repo;
    private final AuditCheckpointRepository checkpointRepo;
    private final SystemAuditIdentity systemAuditIdentity;

    @Override
    @Transactional
    public void store(SignedAction a) {
        var e = new SignedActionEntity();
        e.setActionId(a.actionId());
        e.setSubject(a.subject());
        e.setAction(a.action());
        e.setResource(a.resource());
        e.setTimestamp(a.timestamp());
        e.setCorrelationId(a.correlationId());
        e.setKeyId(a.keyId());
        e.setMetadata(a.metadata()); // jsonb (Map<String,Object>)
        e.setPreviousHash(a.previousHash());
        e.setSequenceNumber(a.sequenceNumber());
        e.setSignature(a.signature());
        repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SignedAction> getLatest(String subject) {
        return repo.findFirstBySubjectOrderBySequenceNumberDesc(subject).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SignedAction> getBySequence(String subject, long seq) {
        return repo.findBySubjectAndSequenceNumber(subject, seq).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignedAction> getRange(String subject, long from, long to) {
        return repo
                .findBySubjectAndSequenceNumberBetweenOrderBySequenceNumberAsc(subject, from, to)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // NOTE: nothing in SP8 invokes storeCheckpoint — AuditCheckpointService is the sole production
    // writer of audit_checkpoints (it persists the full entity, including key_id, directly via
    // AuditCheckpointRepository). This override exists only to satisfy the AuditChainStore SPI
    // contract and to back the store/get round-trip test; it sets key_id from the SYSTEM_AUDIT
    // identity so the FK to user_key_pairs is satisfiable. Do not build a second persistence path.
    @Override
    @Transactional
    public void storeCheckpoint(AuditCheckpoint c) {
        var e = new AuditCheckpointEntity();
        e.setCheckpointId(c.checkpointId());
        e.setGlobalSequence(c.globalSequence());
        e.setTimestamp(c.timestamp());
        e.setChainHeads(c.chainHeads());
        e.setMerkleRoot(c.merkleRoot());
        e.setSignature(c.signature());
        e.setKeyId(systemAuditIdentity.keyId());
        checkpointRepo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditCheckpoint> getLatestCheckpoint() {
        return checkpointRepo
                .findFirstByOrderByGlobalSequenceDescTimestampDesc()
                .map(this::toCheckpoint);
    }

    // The AuditCheckpoint record carries no keyId, so the entity's key_id is dropped on mapping.
    private AuditCheckpoint toCheckpoint(AuditCheckpointEntity e) {
        return new AuditCheckpoint(
                e.getCheckpointId(),
                e.getGlobalSequence(),
                e.getTimestamp(),
                e.getChainHeads(),
                e.getMerkleRoot(),
                e.getSignature());
    }

    private SignedAction toDomain(SignedActionEntity e) {
        return new SignedAction(
                e.getActionId(),
                e.getSubject(),
                e.getAction(),
                e.getResource(),
                e.getTimestamp(),
                e.getCorrelationId(),
                e.getKeyId(),
                e.getMetadata(),
                e.getPreviousHash(),
                e.getSequenceNumber(),
                e.getSignature());
    }
}
