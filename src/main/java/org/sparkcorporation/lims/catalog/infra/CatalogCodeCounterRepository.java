package org.sparkcorporation.lims.catalog.infra;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link CatalogCodeCounter}.
 *
 * <p>{@link #findByIdForUpdate(String)} acquires a pessimistic write lock (PostgreSQL {@code SELECT
 * ... FOR UPDATE}) so concurrent code-generation calls serialize on the counter row — see spec §4.6
 * for the divergence vs the patient module's {@code INSERT ... ON CONFLICT ... RETURNING} pattern.
 */
public interface CatalogCodeCounterRepository extends JpaRepository<CatalogCodeCounter, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CatalogCodeCounter c WHERE c.entityType = :type")
    Optional<CatalogCodeCounter> findByIdForUpdate(@Param("type") String entityType);
}
