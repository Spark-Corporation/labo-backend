package org.sparkcorporation.lims.iam.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserKeyPairRepository extends JpaRepository<UserKeyPairEntity, String> {

    Optional<UserKeyPairEntity> findFirstBySubjectAndActiveTrue(String subject);

    List<UserKeyPairEntity> findBySubjectOrderByCreatedAtDesc(String subject);

    @Modifying
    @Query("update UserKeyPairEntity k set k.active=false where k.subject=:s and k.active=true")
    int deactivateAll(@Param("s") String subject);
}
