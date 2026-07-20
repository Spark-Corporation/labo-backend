package org.sparkcorporation.lims.iam.infra;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditCheckpointRepository extends JpaRepository<AuditCheckpointEntity, String> {
    Optional<AuditCheckpointEntity> findFirstByOrderByGlobalSequenceDescTimestampDesc();
}
