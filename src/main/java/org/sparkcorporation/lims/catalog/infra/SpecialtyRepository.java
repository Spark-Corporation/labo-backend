package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.Specialty;

/**
 * Spring Data JPA repository for {@link Specialty}. See {@link DisciplineRepository} for
 * soft-delete semantics: standard finders still see soft-deleted rows; active-only callers must use
 * {@link #findActiveById(String)} / {@link #findAllActive()}.
 */
public interface SpecialtyRepository extends JpaRepository<Specialty, String> {

    Optional<Specialty> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT s FROM Specialty s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Specialty> findActiveById(@Param("id") String id);

    @Query("SELECT s FROM Specialty s WHERE s.deletedAt IS NULL ORDER BY s.code")
    List<Specialty> findAllActive();
}
