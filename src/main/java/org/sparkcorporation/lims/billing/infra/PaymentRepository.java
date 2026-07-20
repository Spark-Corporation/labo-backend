package org.sparkcorporation.lims.billing.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.billing.domain.Payment;

/**
 * Spring Data JPA repository for {@link Payment}. Two query methods:
 *
 * <ul>
 *   <li>{@link #findByLabFileIdOrderByRecordedAtAsc} backs the {@code GET
 *       /api/v1/lab-files/{id}/payments} endpoint and the IT smoke check.
 *   <li>{@link #linkPaymentsToInvoice} is the bulk back-fill called by {@code
 *       BillingFinalizationService} once the invoice for a lab-file is finalized — sets {@code
 *       invoice_id} on every payment for the lab-file that still has a null link.
 * </ul>
 */
public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByLabFileIdOrderByRecordedAtAsc(String labFileId);

    @Modifying
    @Query(
            """
            UPDATE Payment p SET p.invoiceId = :invoiceId
            WHERE p.labFileId = :labFileId AND p.invoiceId IS NULL
            """)
    int linkPaymentsToInvoice(
            @Param("labFileId") String labFileId, @Param("invoiceId") String invoiceId);
}
