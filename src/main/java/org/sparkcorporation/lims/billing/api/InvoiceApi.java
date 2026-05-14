package org.sparkcorporation.lims.billing.api;

import java.time.Instant;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Cross-module read interface for the {@link org.sparkcorporation.lims.billing.domain.Invoice}
 * aggregate. Other modules ({@code accounting}, {@code stats}, etc.) import {@code billing.api}
 * only — they never reach into {@code billing.domain} or {@code billing.infra} directly.
 */
public interface InvoiceApi {

    Optional<InvoiceSummary> findById(InvoiceId id);

    /** 1:1 with LabFile (UNIQUE constraint on {@code invoices.lab_file_id}). */
    Optional<InvoiceSummary> findByLabFileId(LabFileId labFileId);

    Optional<InvoiceSummary> findByNumber(InvoiceNumber number);

    /**
     * Search projection per spec §7.2 {@code GET /invoices}. All filters are optional; nulls match
     * any value. The {@code patientId} filter resolves through {@code lab_files.patient_id} (a
     * join, since {@code invoices} only stores customer-kind/customer-id snapshots).
     */
    Page<InvoiceSummary> search(
            @Nullable Instant from,
            @Nullable Instant to,
            @Nullable InvoiceCustomerKind customerKind,
            @Nullable String customerId,
            @Nullable LabFileId labFileId,
            @Nullable String patientId,
            Pageable pageable);
}
