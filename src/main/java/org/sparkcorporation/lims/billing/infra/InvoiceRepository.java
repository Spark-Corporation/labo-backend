package org.sparkcorporation.lims.billing.infra;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;
import org.sparkcorporation.lims.billing.domain.Invoice;

/**
 * Spring Data JPA repository for {@link Invoice}. Reads project the aggregate to {@code
 * InvoiceSummary} in {@code DefaultInvoiceApi} — this layer stays close to entity semantics.
 *
 * <p>The {@code patientId} filter cannot be expressed in this repo (would require reaching into
 * {@code order.domain} which violates the modulith boundary). {@code DefaultInvoiceApi} resolves
 * the patient → lab-file-ids list via {@code LabFileApi.findActiveByPatient(...)} first, then
 * passes them through {@link #search(Instant, Instant, InvoiceCustomerKind, String, Collection,
 * Pageable)}.
 */
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    Optional<Invoice> findByLabFileId(String labFileId);

    Optional<Invoice> findByNumber(String number);

    List<Invoice> findAllByLabFileIdIn(Collection<String> labFileIds);

    @Query(
            """
            SELECT i FROM Invoice i
            WHERE (CAST(:from AS java.time.Instant) IS NULL OR i.issuedAt >= :from)
              AND (CAST(:to AS java.time.Instant) IS NULL OR i.issuedAt < :to)
              AND (:customerKind IS NULL OR i.customerKind = :customerKind)
              AND (:customerId IS NULL OR i.customerId = :customerId)
              AND (:#{#labFileIds == null} = true OR i.labFileId IN :labFileIds)
            ORDER BY i.issuedAt DESC
            """)
    Page<Invoice> search(
            @Param("from") @Nullable Instant from,
            @Param("to") @Nullable Instant to,
            @Param("customerKind") @Nullable InvoiceCustomerKind customerKind,
            @Param("customerId") @Nullable String customerId,
            @Param("labFileIds") @Nullable Collection<String> labFileIds,
            Pageable pageable);
}
