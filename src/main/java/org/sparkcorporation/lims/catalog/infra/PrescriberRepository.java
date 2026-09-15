package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.Prescriber;

/**
 * Spring Data JPA repository for {@link Prescriber}. Active-finders use {@code LEFT JOIN FETCH p
 * .specialty} to avoid the cross-module N+1 when callers eagerly project a {@code
 * PrescriberSummary.SpecialtySnapshot}.
 *
 * <p>See {@link DisciplineRepository} for soft-delete semantics. The LEFT JOIN does not filter
 * {@code specialty.deleted_at IS NULL} — historical FK integrity from past prescriber records is
 * preserved across specialty soft deletes.
 */
public interface PrescriberRepository extends JpaRepository<Prescriber, String> {

    Optional<Prescriber> findByCode(String code);

    boolean existsByCode(String code);

    @Query(
            "SELECT p FROM Prescriber p LEFT JOIN FETCH p.specialty"
                    + " WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Prescriber> findActiveById(@Param("id") String id);

    @Query(
            "SELECT p FROM Prescriber p LEFT JOIN FETCH p.specialty"
                    + " WHERE p.deletedAt IS NULL ORDER BY p.name")
    List<Prescriber> findAllActive();

    @Query(
            """
            SELECT p FROM Prescriber p LEFT JOIN FETCH p.specialty s
            WHERE p.deletedAt IS NULL
              AND (:nameQuery IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:nameQuery AS String), '%')))
              AND (:specialtyCode IS NULL OR s.code = :specialtyCode)
            ORDER BY p.name
            """)
    List<Prescriber> search(
            @Param("nameQuery") @Nullable String nameQuery,
            @Param("specialtyCode") @Nullable String specialtyCode);
}
