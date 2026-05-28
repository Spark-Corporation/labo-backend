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

    @Override
    public void storeCheckpoint(AuditCheckpoint checkpoint) {
        // Checkpointing deferred to SP8 — intentional no-op.
    }

    @Override
    public Optional<AuditCheckpoint> getLatestCheckpoint() {
        return Optional.empty();
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
