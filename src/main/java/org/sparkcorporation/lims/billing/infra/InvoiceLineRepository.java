package org.sparkcorporation.lims.billing.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import org.sparkcorporation.lims.billing.domain.InvoiceLine;

/**
 * Spring Data JPA repository for {@link InvoiceLine}. The aggregate's {@code @OneToMany} cascade
 * covers persistence, but a dedicated repo is available for projection / search queries that
 * outgrow the parent.
 */
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, String> {}
