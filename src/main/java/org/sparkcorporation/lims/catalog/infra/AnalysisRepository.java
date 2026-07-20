package org.sparkcorporation.lims.catalog.infra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.domain.Analysis;

/**
 * Spring Data JPA repository for {@link Analysis}. Active-finders use {@code LEFT JOIN FETCH} on
 * {@code discipline}, {@code matrix}, {@code recipient} to avoid the cross-module N+1 when callers
 * eagerly project {@code AnalysisSummary} snapshots.
 *
 * <p><b>Multi-bag fetch avoidance.</b> Hibernate throws {@code MultipleBagFetchException} when JOIN
 * FETCH spans more than one {@code List}-typed collection. The {@code components} (List) and {@code
 * techniques} (Set) collections are deliberately NOT joined here — {@link DefaultAnalysisApi}
 * resolves them via {@link #findChildrenForParent(String)} and {@link #findTechniqueLinks(String)}
 * batched lookups instead. This trades one extra round-trip for predictable behavior across all
 * queries.
 *
 * <p>See {@link DisciplineRepository} for soft-delete semantics. The LEFT JOINs do not filter
 * {@code deleted_at IS NULL} on the joined entity — historical FK integrity from past lab-file
 * records is preserved across catalog soft deletes.
 */
public interface AnalysisRepository extends JpaRepository<Analysis, String> {

    Optional<Analysis> findByCode(String code);

    @Query(
            """
            SELECT a FROM Analysis a
                LEFT JOIN FETCH a.discipline
                LEFT JOIN FETCH a.matrix
                LEFT JOIN FETCH a.recipient
            WHERE a.id = :id AND a.deletedAt IS NULL
            """)
    Optional<Analysis> findActiveById(@Param("id") String id);

    @Query(
            """
            SELECT a FROM Analysis a
                LEFT JOIN FETCH a.discipline
                LEFT JOIN FETCH a.matrix
                LEFT JOIN FETCH a.recipient
            WHERE a.deletedAt IS NULL
            ORDER BY a.code
            """)
    List<Analysis> findAllActive();

    @Query(
            """
            SELECT a FROM Analysis a
                LEFT JOIN FETCH a.discipline d
                LEFT JOIN FETCH a.matrix
                LEFT JOIN FETCH a.recipient
            WHERE a.deletedAt IS NULL
              AND (:nameQuery IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :nameQuery, '%')))
              AND (:disciplineCode IS NULL OR d.code = :disciplineCode)
              AND (:kind IS NULL OR a.kind = :kind)
            ORDER BY a.code
            """)
    List<Analysis> search(
            @Param("nameQuery") @Nullable String nameQuery,
            @Param("disciplineCode") @Nullable String disciplineCode,
            @Param("kind") @Nullable AnalysisKind kind);

    @Query(
            """
            SELECT a FROM Analysis a
                LEFT JOIN FETCH a.discipline
                LEFT JOIN FETCH a.matrix
                LEFT JOIN FETCH a.recipient
            WHERE a.deletedAt IS NULL AND a.discipline.id = :disciplineId
            ORDER BY a.code
            """)
    List<Analysis> findActiveByDiscipline(@Param("disciplineId") String disciplineId);

    /**
     * Batched active-lookup by ids — used by {@code billing} invoice finalization to resolve all
     * analyses on a lab-file in one round-trip. Soft-deleted rows are excluded.
     */
    @Query(
            """
            SELECT a FROM Analysis a
                LEFT JOIN FETCH a.discipline
                LEFT JOIN FETCH a.matrix
                LEFT JOIN FETCH a.recipient
            WHERE a.id IN :ids AND a.deletedAt IS NULL
            """)
    List<Analysis> findAllActiveByIdIn(@Param("ids") Collection<String> ids);

    /**
     * Batched resolution of (childId, childCode, childName, position) for a parent's components.
     * Used by {@link DefaultAnalysisApi} to project {@code AnalysisSummary.ComponentLink} rows
     * without N+1 round-trips. Children that are themselves soft-deleted are excluded so the
     * summary never surfaces orphaned links.
     */
    @Query(
            """
            SELECT ac.childId, c.code, c.name, ac.position
            FROM AnalysisComponent ac, Analysis c
            WHERE ac.parentId = :parentId
              AND c.id = ac.childId
              AND c.deletedAt IS NULL
            ORDER BY ac.position ASC
            """)
    List<Object[]> findChildrenForParent(@Param("parentId") String parentId);

    /**
     * Batched resolution of (techniqueId, techniqueCode, techniqueName, isDefault) for an
     * analysis's technique links. Soft-deleted techniques are excluded.
     */
    @Query(
            """
            SELECT atl.techniqueId, t.code, t.name, atl.isDefault
            FROM AnalysisTechniqueLink atl, Technique t
            WHERE atl.analysisId = :analysisId
              AND t.id = atl.techniqueId
              AND t.deletedAt IS NULL
            ORDER BY t.code ASC
            """)
    List<Object[]> findTechniqueLinks(@Param("analysisId") String analysisId);
}
