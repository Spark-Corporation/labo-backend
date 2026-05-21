package org.sparkcorporation.lims.pdf.infra;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PdfRenderJobRepository extends JpaRepository<PdfRenderJob, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    UPDATE pdf_render_jobs
                       SET status = 'RUNNING'
                     WHERE id = :id AND status = 'QUEUED'
                    """,
            nativeQuery = true)
    int flipQueuedToRunning(@Param("id") String id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    UPDATE pdf_render_jobs
                       SET status = 'DONE',
                           bytes = :bytes,
                           suggested_filename = :filename,
                           completed_at = :completedAt
                     WHERE id = :id AND status = 'RUNNING'
                    """,
            nativeQuery = true)
    int markDone(
            @Param("id") String id,
            @Param("bytes") byte[] bytes,
            @Param("filename") String filename,
            @Param("completedAt") Instant completedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    UPDATE pdf_render_jobs
                       SET status = 'FAILED',
                           error_message = :errorMessage,
                           completed_at = :completedAt
                     WHERE id = :id AND status = 'RUNNING'
                    """,
            nativeQuery = true)
    int markFailed(
            @Param("id") String id,
            @Param("errorMessage") String errorMessage,
            @Param("completedAt") Instant completedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    DELETE FROM pdf_render_jobs
                     WHERE status IN ('DONE','FAILED') AND completed_at < :cutoff
                    """,
            nativeQuery = true)
    int deleteCompletedBefore(@Param("cutoff") Instant cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    UPDATE pdf_render_jobs
                       SET status = 'FAILED',
                           error_message = :reason,
                           completed_at = :now
                     WHERE status IN ('QUEUED','RUNNING') AND requested_at < :cutoff
                    """,
            nativeQuery = true)
    int flipOrphanedRunningToFailed(
            @Param("cutoff") Instant cutoff,
            @Param("reason") String reason,
            @Param("now") Instant now);
}
