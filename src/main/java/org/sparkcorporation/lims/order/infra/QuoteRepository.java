package org.sparkcorporation.lims.order.infra;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.order.domain.Quote;

/**
 * Spring Data JPA repository for {@link Quote}. The generic ID parameter is {@code String} (the raw
 * ULID value); cross-module callers pass {@code QuoteId} into {@code DefaultQuoteApi} which
 * extracts {@code .value()} before delegating here.
 *
 * <p><b>Soft-delete semantics.</b> {@link Quote} uses {@code @SQLDelete} only (NO
 * {@code @SQLRestriction}, per spec §4.4) so historical FK integrity from converted lab files is
 * preserved. Active-finders therefore explicitly filter {@code q.deletedAt IS NULL}.
 *
 * <p><b>Multi-bag fetch handling.</b> Per spec §6.3 — {@link #findActiveById(String)} loads the
 * quote row only; {@code DefaultQuoteApi} resolves the {@code lines} bag via {@link
 * QuoteLineRepository#findByQuoteId(String)} (one extra round-trip, no N+1; mirrors {@link
 * LabFileRepository}'s multi-bag-fetch split for shape consistency).
 */
public interface QuoteRepository extends JpaRepository<Quote, String> {

    @Query(
            """
            SELECT q FROM Quote q
             WHERE q.id = :id AND q.deletedAt IS NULL
            """)
    Optional<Quote> findActiveById(@Param("id") String id);

    @Query(
            """
            SELECT q FROM Quote q
             WHERE q.quoteNumber.value = :qn AND q.deletedAt IS NULL
            """)
    Optional<Quote> findActiveByQuoteNumber(@Param("qn") String qn);

    @Query(
            """
            SELECT q FROM Quote q
             WHERE q.patientId = :patientId AND q.deletedAt IS NULL
             ORDER BY q.createdAt DESC
            """)
    List<Quote> findActiveByPatientId(@Param("patientId") String patientId);

    @Query(
            """
            SELECT q FROM Quote q
             WHERE q.deletedAt IS NULL
               AND (:patientId IS NULL OR q.patientId = :patientId)
               AND (:prescriberId IS NULL OR q.prescriberId = :prescriberId)
               AND (:status IS NULL OR q.status = :status)
               AND (:from IS NULL OR q.createdAt >= :from)
               AND (:to IS NULL OR q.createdAt <  :to)
             ORDER BY q.createdAt DESC
            """)
    Page<Quote> search(
            @Param("patientId") @Nullable String patientId,
            @Param("prescriberId") @Nullable String prescriberId,
            @Param("status") @Nullable QuoteStatus status,
            @Param("from") @Nullable Instant from,
            @Param("to") @Nullable Instant to,
            Pageable pageable);
}
