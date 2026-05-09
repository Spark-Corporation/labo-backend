package org.sparkcorporation.lims.order.api;

import java.util.List;
import java.util.Optional;

/**
 * Public, read-only Quote interface for cross-module consumption. Other modules import {@code
 * order.api} only — they never reach into {@code order.domain} or {@code order.infra} directly.
 */
public interface QuoteApi {

    Optional<QuoteSummary> findById(QuoteId id);

    Optional<QuoteSummary> findByQuoteNumber(QuoteNumber quoteNumber);

    /**
     * @throws org.sparkcorporation.lims.order.domain.QuoteNotFoundException if no match
     */
    QuoteSummary requireById(QuoteId id);

    /** Active-only existence check (filters soft-deleted). */
    boolean existsById(QuoteId id);

    List<QuoteSummary> findActiveByPatient(String patientId);
}
