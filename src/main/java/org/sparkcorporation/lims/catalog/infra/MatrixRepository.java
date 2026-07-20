package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.Matrix;

/**
 * Spring Data JPA repository for {@link Matrix}. See {@link DisciplineRepository} for soft-delete
 * semantics.
 */
public interface MatrixRepository extends JpaRepository<Matrix, String> {

    Optional<Matrix> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT m FROM Matrix m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Matrix> findActiveById(@Param("id") String id);

    @Query("SELECT m FROM Matrix m WHERE m.deletedAt IS NULL")
    Page<Matrix> findAllActive(Pageable pageable);

    @Query("SELECT m FROM Matrix m WHERE m.deletedAt IS NULL ORDER BY m.code")
    List<Matrix> findAllActiveOrderByCode();
}
