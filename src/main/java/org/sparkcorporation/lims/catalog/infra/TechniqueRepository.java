package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.Technique;

/**
 * Spring Data JPA repository for {@link Technique}. See {@link DisciplineRepository} for
 * soft-delete semantics.
 */
public interface TechniqueRepository extends JpaRepository<Technique, String> {

    Optional<Technique> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT t FROM Technique t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<Technique> findActiveById(@Param("id") String id);

    @Query("SELECT t FROM Technique t WHERE t.deletedAt IS NULL")
    Page<Technique> findAllActive(Pageable pageable);

    @Query("SELECT t FROM Technique t WHERE t.deletedAt IS NULL ORDER BY t.code")
    List<Technique> findAllActiveOrderByCode();
}
