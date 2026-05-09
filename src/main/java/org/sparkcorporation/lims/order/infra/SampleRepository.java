package org.sparkcorporation.lims.order.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import org.sparkcorporation.lims.order.domain.Sample;

/**
 * Spring Data JPA repository for the sealed {@link Sample} hierarchy (single-table inheritance with
 * {@code sample_type} discriminator).
 *
 * <p>{@link #findByLabFileId(String)} is the second round-trip of the multi-bag-fetch split — see
 * {@link LabFileRepository} class-doc and spec §6.3.
 */
public interface SampleRepository extends JpaRepository<Sample, String> {

    List<Sample> findByLabFileId(String labFileId);
}
