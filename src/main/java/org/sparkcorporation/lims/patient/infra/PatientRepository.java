package org.sparkcorporation.lims.patient.infra;

import java.util.Optional;

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

    @Query(
            """
            SELECT p FROM Patient p
            WHERE (:labCode IS NULL OR p.labCode = :labCode)
              AND (:phone IS NULL OR p.contact.phone = :phone)
              AND (:idDocumentNumber IS NULL OR p.idDocument.number = :idDocumentNumber)
              AND (:nameLike IS NULL OR LOWER(p.name.lastName) LIKE LOWER(CONCAT('%', :nameLike, '%')))
            """)
    Page<Patient> search(
            @Param("nameLike") @org.springframework.lang.Nullable String nameLike,
            @Param("phone") @org.springframework.lang.Nullable String phone,
            @Param("idDocumentNumber") @org.springframework.lang.Nullable String idDocumentNumber,
            @Param("labCode") @org.springframework.lang.Nullable String labCode,
            Pageable pageable);
}
