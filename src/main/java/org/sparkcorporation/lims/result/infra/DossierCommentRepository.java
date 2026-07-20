package org.sparkcorporation.lims.result.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import org.sparkcorporation.lims.result.domain.DossierComment;

/**
 * Spring Data JPA repository for {@link DossierComment}. Per spec §4.4 the natural key {@code
 * lab_file_id} is the PK, so {@code findById(String)} works directly without a custom query.
 *
 * <p>{@link DossierComment} is not soft-deleted (chunk 2 / §9.3) — hard-deletes mirror the CDC
 * "comment-block can be cleared" semantics, so no {@code findActive*} variants are needed.
 */
public interface DossierCommentRepository extends JpaRepository<DossierComment, String> {}
