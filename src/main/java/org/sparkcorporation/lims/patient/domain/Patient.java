package org.sparkcorporation.lims.patient.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Patient aggregate. JPA entity with soft-delete via Hibernate {@link SQLDelete} + {@link
 * SQLRestriction}; the {@code @SQLDelete} statement includes the version check to preserve
 * optimistic-lock semantics on concurrent deletes.
 */
@Entity
@Table(name = "patients")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(
        sql =
                "UPDATE patients SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Patient {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(name = "lab_code", length = 30, nullable = false, unique = true)
    private String labCode;

    @Embedded private PersonName name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Civility civility;

    @Enumerated(EnumType.STRING)
    @Column(length = 1, nullable = false)
    private Sex sex;

    @Column(name = "birth_date")
    @Nullable
    private LocalDate birthDate;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "type", column = @Column(name = "id_document_type", length = 20)),
        @AttributeOverride(
                name = "number",
                column = @Column(name = "id_document_number", length = 40))
    })
    @Nullable
    private IdDocument idDocument;

    @Column(length = 80)
    @Nullable
    private String profession;

    @Column(length = 255)
    @Nullable
    private String address;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    @Nullable
    private String clinicalNotes;

    @Embedded private ContactInfo contact;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "guardian_name", length = 120)),
        @AttributeOverride(name = "phone", column = @Column(name = "guardian_phone", length = 30)),
        @AttributeOverride(
                name = "relationship",
                column = @Column(name = "guardian_relationship", length = 20))
    })
    @Nullable
    private Guardian guardian;

    @Column(name = "insurance_id", length = 26)
    @Nullable
    private String insuranceId;

    @Column(name = "partner_id", length = 26)
    @Nullable
    private String partnerId;

    @Column(name = "portal_login", length = 80, unique = true)
    @Nullable
    private String portalLogin;

    @Column(name = "portal_password_hash", length = 255)
    @Nullable
    private String portalPasswordHash;

    @Column(name = "merged_into_id", columnDefinition = "varchar(26)")
    @Nullable
    private String mergedIntoId;

    @Column(name = "deleted_at")
    @Nullable
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 26)
    @Nullable
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 26)
    @Nullable
    private String updatedBy;

    @Version private int version;

    protected Patient() {}

    public static Patient create(NewPatient cmd, LabCode generated) {
        var p = new Patient();
        p.id = Ulid.generate();
        p.labCode = generated.value();
        p.name = cmd.name();
        p.civility = cmd.civility();
        p.sex = cmd.sex();
        p.birthDate = cmd.birthDate();
        p.idDocument = cmd.idDocument();
        p.profession = cmd.profession();
        p.address = cmd.address();
        p.clinicalNotes = cmd.clinicalNotes();
        p.contact = cmd.contact();
        p.guardian = cmd.guardian();
        p.insuranceId = cmd.insuranceId();
        p.partnerId = cmd.partnerId();
        p.portalLogin = cmd.portalLogin();
        p.portalPasswordHash = cmd.portalPasswordHash();
        return p;
    }

    public void update(UpdatePatient cmd) {
        if (cmd.name() != null) this.name = cmd.name();
        if (cmd.civility() != null) this.civility = cmd.civility();
        if (cmd.sex() != null) this.sex = cmd.sex();
        if (cmd.birthDate() != null) this.birthDate = cmd.birthDate();
        if (cmd.idDocument() != null) this.idDocument = cmd.idDocument();
        if (cmd.profession() != null) this.profession = cmd.profession();
        if (cmd.address() != null) this.address = cmd.address();
        if (cmd.clinicalNotes() != null) this.clinicalNotes = cmd.clinicalNotes();
        if (cmd.contact() != null) this.contact = cmd.contact();
        if (cmd.guardian() != null) this.guardian = cmd.guardian();
        if (cmd.insuranceId() != null) this.insuranceId = cmd.insuranceId();
        if (cmd.partnerId() != null) this.partnerId = cmd.partnerId();
        if (cmd.portalLogin() != null) this.portalLogin = cmd.portalLogin();
        if (cmd.portalPasswordHash() != null) this.portalPasswordHash = cmd.portalPasswordHash();
    }

    public void mergeInto(PatientId targetId) {
        if (this.mergedIntoId != null) {
            throw new IllegalStateException("patient already merged: " + this.id);
        }
        this.mergedIntoId = targetId.value();
    }

    public PatientId id() {
        return new PatientId(id);
    }

    String rawId() {
        return id;
    }

    public String labCode() {
        return labCode;
    }

    public PersonName name() {
        return name;
    }

    public Civility civility() {
        return civility;
    }

    public Sex sex() {
        return sex;
    }

    public @Nullable LocalDate birthDate() {
        return birthDate;
    }

    public @Nullable IdDocument idDocument() {
        return idDocument;
    }

    public @Nullable String profession() {
        return profession;
    }

    public @Nullable String address() {
        return address;
    }

    public @Nullable String clinicalNotes() {
        return clinicalNotes;
    }

    public ContactInfo contact() {
        return contact;
    }

    public @Nullable Guardian guardian() {
        return guardian;
    }

    public @Nullable String insuranceId() {
        return insuranceId;
    }

    public @Nullable String partnerId() {
        return partnerId;
    }

    public @Nullable String portalLogin() {
        return portalLogin;
    }

    public @Nullable String portalPasswordHash() {
        return portalPasswordHash;
    }

    public @Nullable String mergedIntoId() {
        return mergedIntoId;
    }

    public @Nullable Instant deletedAt() {
        return deletedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public int version() {
        return version;
    }

    public boolean isMerged() {
        return mergedIntoId != null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
