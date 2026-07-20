package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.Preleveur;

/**
 * Spring Data JPA repository for {@link Preleveur}. See {@link DisciplineRepository} for
 * soft-delete semantics. The {@code habilitations} {@code @ElementCollection} is fetched eagerly by
 * the entity mapping; no LEFT JOIN FETCH needed here.
 */
public interface PreleveurRepository extends JpaRepository<Preleveur, String> {

    Optional<Preleveur> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT p FROM Preleveur p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Preleveur> findActiveById(@Param("id") String id);

    @Query("SELECT p FROM Preleveur p WHERE p.deletedAt IS NULL ORDER BY p.name")
    List<Preleveur> findAllActive();
}
