package org.sparkcorporation.lims.result.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.result.domain.Result;

/**
 * Spring Data JPA repository for {@link Result}. The generic ID parameter is {@code String} (the
 * raw ULID value); cross-module callers pass {@code ResultId} into {@link DefaultResultApi} which
 * extracts {@code .value()} before delegating here.
 *
 * <p><b>Soft-delete semantics.</b> {@link Result} uses {@code @SQLDelete} only (NO
 * {@code @SQLRestriction}, per spec §9.3 / CLAUDE.md). Active-finders therefore explicitly filter
 * {@code r.deletedAt IS NULL}.
 *
 * <p><b>Multi-bag fetch avoidance.</b> Per spec §16.6 the two child collections are typed as {@code
 * Set<...>} (with {@code @OrderBy("position ASC")}) so a single {@code @EntityGraph} can fetch both
 * eagerly without triggering Hibernate's {@code MultipleBagFetchException}. This avoids the
 * extra-round-trip pattern that {@code order::infra.LabFileRepository} uses (where the bags are
 * {@code List}).
 */
public interface ResultRepository extends JpaRepository<Result, String> {

    @EntityGraph(attributePaths = {"lines", "antibiogramReadings"})
    @Query(
            """
            SELECT r FROM Result r
             WHERE r.id = :id AND r.deletedAt IS NULL
            """)
    Optional<Result> findActiveById(@Param("id") String id);

    @Query("SELECT count(r) > 0 FROM Result r WHERE r.id = :id AND r.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") String id);

    @EntityGraph(attributePaths = {"lines", "antibiogramReadings"})
    @Query(
            """
            SELECT r FROM Result r
             WHERE r.analysisRequestId = :arId AND r.deletedAt IS NULL
            """)
    Optional<Result> findActiveByAnalysisRequestId(@Param("arId") String analysisRequestId);

    @EntityGraph(attributePaths = {"lines", "antibiogramReadings"})
    @Query(
            """
            SELECT r FROM Result r
             WHERE r.labFileId = :labFileId AND r.deletedAt IS NULL
             ORDER BY r.analysisCodeSnapshot
            """)
    List<Result> findActiveByLabFileId(@Param("labFileId") String labFileId);
}
