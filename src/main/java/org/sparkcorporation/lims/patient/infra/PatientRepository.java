package org.sparkcorporation.lims.patient.infra;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.patient.domain.Patient;

/**
 * Spring Data JPA repository for {@link Patient}. The generic ID parameter is {@code String} (the
 * raw ULID value); cross-module callers pass {@code PatientId} into {@code DefaultPatientApi} which
 * extracts {@code .value()} before delegating here.
 *
 * <p>Soft delete via {@code @SQLRestriction} is automatic: standard finder methods only return
 * non-deleted rows.
 */
public interface PatientRepository extends JpaRepository<Patient, String> {

    Optional<Patient> findByLabCode(String labCode);

    Optional<Patient> findByPortalLogin(String portalLogin);

    /**
     * Caller is expected to wrap {@code nameLike} with leading/trailing {@code %} and lowercase it
     * before invoking — this keeps the LOWER() call out of the parameterized side of the LIKE
     * expression. Hibernate 6+/Postgres bind a null String through {@code setObject(idx, null)}
     * which the driver infers as bytea, so {@code LOWER(?)} parses as {@code LOWER(bytea)} (no such
     * function) at plan-time even when the {@code IS NULL} short-circuit guards execution.
     * Lowercasing in Java side-steps the parser ambiguity entirely.
     */
    @Query(
            """
            SELECT p FROM Patient p
             WHERE p.id IN :ids AND p.deletedAt IS NULL
            """)
    List<Patient> findAllActiveByIds(@Param("ids") Set<String> ids);

    @Query(
            """
            SELECT p FROM Patient p
            WHERE (:labCode IS NULL OR p.labCode = :labCode)
              AND (:phone IS NULL OR p.contact.phone = :phone)
              AND (:idDocumentNumber IS NULL OR p.idDocument.number = :idDocumentNumber)
              AND (:nameLikeLower IS NULL OR LOWER(p.name.lastName) LIKE :nameLikeLower)
            """)
    Page<Patient> search(
            @Param("nameLikeLower") @Nullable String nameLikeLower,
            @Param("phone") @Nullable String phone,
            @Param("idDocumentNumber") @Nullable String idDocumentNumber,
            @Param("labCode") @Nullable String labCode,
            Pageable pageable);
}
