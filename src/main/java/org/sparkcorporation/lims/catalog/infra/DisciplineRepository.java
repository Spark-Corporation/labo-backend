package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.Discipline;

/**
 * Spring Data JPA repository for {@link Discipline}. The generic ID parameter is {@code String}
 * (the raw ULID value). Cross-module callers go through {@code DefaultDisciplineApi} which extracts
 * {@code .value()} before delegating here.
 *
 * <p>Soft delete is via Hibernate {@code @SQLDelete}; standard finder methods (e.g. {@code
 * findById}) still return rows where {@code deleted_at IS NOT NULL}. Active-only callers must use
 * the {@code findActive*} variants below.
 */
public interface DisciplineRepository extends JpaRepository<Discipline, String> {

    Optional<Discipline> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT d FROM Discipline d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Discipline> findActiveById(@Param("id") String id);

    @Query("SELECT d FROM Discipline d WHERE d.deletedAt IS NULL")
    Page<Discipline> findAllActive(Pageable pageable);

    @Query("SELECT d FROM Discipline d WHERE d.deletedAt IS NULL ORDER BY d.code")
    List<Discipline> findAllActiveOrderByCode();
}
