package org.sparkcorporation.lims.catalog.domain;

import java.time.Clock;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.SQLDelete;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.SpecialtyCode;
import org.sparkcorporation.lims.catalog.api.SpecialtyId;

/**
 * Specialty (specialité des prescripteurs) aggregate. JPA entity with soft delete via Hibernate
 * {@link SQLDelete}.
 *
 * <p>NOTE: deliberately NO {@code @SQLRestriction} — historical FK integrity from {@code
 * prescribers.specialty_id} is preserved across soft deletes. Repository methods explicitly filter
 * {@code deleted_at IS NULL}.
 */
@Entity
@Table(name = "specialties")
@SQLDelete(
        sql =
                "UPDATE specialties SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
public class Specialty {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(length = 30, nullable = false, unique = true)
    private String code;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(length = 255)
    @Nullable
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "deleted_at")
    @Nullable
    private Instant deletedAt;

    protected Specialty() {}

    public static Specialty create(NewSpecialty cmd, Clock clock) {
        var s = new Specialty();
        s.id = SpecialtyId.generate().value();
        s.code = SpecialtyCode.of(cmd.code()).value();
        s.name = cmd.name();
        s.description = cmd.description();
        var now = Instant.now(clock);
        s.createdAt = now;
        s.updatedAt = now;
        return s;
    }

    public void updateNameAndDescription(UpdateSpecialty cmd, Clock clock) {
        if (cmd.name() != null) this.name = cmd.name();
        if (cmd.description() != null) this.description = cmd.description();
        this.updatedAt = Instant.now(clock);
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public SpecialtyId id() {
        return SpecialtyId.of(id);
    }

    public String rawId() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public @Nullable String description() {
        return description;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    public @Nullable Instant deletedAt() {
        return deletedAt;
    }
}
