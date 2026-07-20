package org.sparkcorporation.lims.iam.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SignedActionRepository
        extends JpaRepository<SignedActionEntity, String>,
                JpaSpecificationExecutor<SignedActionEntity> {

    Optional<SignedActionEntity> findFirstBySubjectOrderBySequenceNumberDesc(String subject);

    @Query("select distinct s.subject from SignedActionEntity s")
    List<String> findDistinctSubjects();

    Optional<SignedActionEntity> findBySubjectAndSequenceNumber(
            String subject, long sequenceNumber);

    List<SignedActionEntity> findBySubjectAndSequenceNumberBetweenOrderBySequenceNumberAsc(
            String subject, long from, long to);

    Page<SignedActionEntity> findBySubject(String subject, Pageable pageable);

    Page<SignedActionEntity> findByResource(String resource, Pageable pageable);

    Page<SignedActionEntity> findByResourceContaining(String labFileId, Pageable pageable);
}
