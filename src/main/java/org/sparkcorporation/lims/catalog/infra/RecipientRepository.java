package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.Recipient;

/**
 * Spring Data JPA repository for {@link Recipient}. The active-finders use {@code LEFT JOIN FETCH}
 * on {@code defaultMatrix} to avoid the cross-module N+1 when callers eagerly project a {@code
 * RecipientSummary.MatrixSnapshot}.
 *
 * <p>See {@link DisciplineRepository} for soft-delete semantics.
 */
public interface RecipientRepository extends JpaRepository<Recipient, String> {

    Optional<Recipient> findByCode(String code);

    boolean existsByCode(String code);

    @Query(
            "SELECT r FROM Recipient r LEFT JOIN FETCH r.defaultMatrix"
                    + " WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<Recipient> findActiveById(@Param("id") String id);

    @Query(
            "SELECT r FROM Recipient r LEFT JOIN FETCH r.defaultMatrix"
                    + " WHERE r.deletedAt IS NULL")
    Page<Recipient> findAllActive(Pageable pageable);

    @Query(
            "SELECT r FROM Recipient r LEFT JOIN FETCH r.defaultMatrix"
                    + " WHERE r.deletedAt IS NULL ORDER BY r.code")
    List<Recipient> findAllActiveOrderByCode();
}
