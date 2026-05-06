package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.Antibiotic;

/**
 * Spring Data JPA repository for {@link Antibiotic}. See {@link DisciplineRepository} for
 * soft-delete semantics.
 */
public interface AntibioticRepository extends JpaRepository<Antibiotic, String> {

    Optional<Antibiotic> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT a FROM Antibiotic a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Antibiotic> findActiveById(@Param("id") String id);

    @Query("SELECT a FROM Antibiotic a WHERE a.deletedAt IS NULL")
    Page<Antibiotic> findAllActive(Pageable pageable);

    @Query("SELECT a FROM Antibiotic a WHERE a.deletedAt IS NULL ORDER BY a.code")
    List<Antibiotic> findAllActiveOrderByCode();
}
