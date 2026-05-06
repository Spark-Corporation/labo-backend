package org.sparkcorporation.lims.catalog.domain;

import java.time.Instant;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.SQLDelete;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Discipline (paillasse) aggregate. JPA entity with soft delete via Hibernate {@link SQLDelete}.
 *
 * <p>NOTE: deliberately NO {@code @SQLRestriction} — historical FK integrity from 1.3.b's Analysis
 * aggregate is preserved. Repository methods explicitly filter {@code deleted_at IS NULL}.
 */
@Entity
@Table(name = "disciplines")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(
        sql =
                "UPDATE disciplines SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
public class Discipline {

    private static final Pattern CODE_FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(length = 30, nullable = false, unique = true)
    private String code;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    @Nullable
    private String description;

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

    protected Discipline() {}

    public static Discipline create(String code, String name, @Nullable String description) {
        if (code == null || !CODE_FORMAT.matcher(code).matches()) {
            throw new IllegalArgumentException("invalid code: " + code);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        var d = new Discipline();
        d.id = Ulid.generate();
        d.code = code;
        d.name = name.trim();
        d.description = description;
        return d;
    }

    public void update(@Nullable String name, @Nullable String description) {
        if (name != null) {
            if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
            this.name = name.trim();
        }
        if (description != null) this.description = description;
    }

    public DisciplineId id() {
        return new DisciplineId(id);
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

    public @Nullable Instant deletedAt() {
        return deletedAt;
    }

    public boolean isActive() {
        return deletedAt == null;
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
}
