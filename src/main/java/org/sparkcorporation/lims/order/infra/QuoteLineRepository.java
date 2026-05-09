package org.sparkcorporation.lims.order.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import org.sparkcorporation.lims.order.domain.QuoteLine;

/**
 * Spring Data JPA repository for {@link QuoteLine}. Used by {@code DefaultQuoteApi} to resolve the
 * {@code lines} bag in a separate round-trip from the parent {@code Quote} row — see {@link
 * QuoteRepository}'s class doc on the multi-bag-fetch split.
 */
public interface QuoteLineRepository extends JpaRepository<QuoteLine, String> {

    List<QuoteLine> findByQuoteId(String quoteId);
}
