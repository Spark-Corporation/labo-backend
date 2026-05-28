package org.sparkcorporation.lims.iam.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignedActionRepository extends JpaRepository<SignedActionEntity, String> {

    Optional<SignedActionEntity> findFirstBySubjectOrderBySequenceNumberDesc(String subject);

    Optional<SignedActionEntity> findBySubjectAndSequenceNumber(
            String subject, long sequenceNumber);

    List<SignedActionEntity> findBySubjectAndSequenceNumberBetweenOrderBySequenceNumberAsc(
            String subject, long from, long to);

    Page<SignedActionEntity> findBySubject(String subject, Pageable pageable);

    Page<SignedActionEntity> findByResource(String resource, Pageable pageable);
}
