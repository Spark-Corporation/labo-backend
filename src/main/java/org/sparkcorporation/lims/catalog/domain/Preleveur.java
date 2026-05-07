package org.sparkcorporation.lims.catalog.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.Habilitation;
import org.sparkcorporation.lims.catalog.api.PreleveurCode;
import org.sparkcorporation.lims.catalog.api.PreleveurId;

/**
 * Préleveur (sample collector / phlebotomist, CDC §5) aggregate. JPA entity with soft delete via
 * Hibernate {@link SQLDelete}.
 *
 * <p>NOTE: deliberately NO {@code @SQLRestriction} — historical FK integrity from 1.4 sample
 * records is preserved across soft deletes. Repository methods explicitly filter {@code deleted_at
 * IS NULL} for active queries.
 *
 * <p>{@code code} is server-generated as {@code PHL-NNNNNN} by {@code CatalogCodeGenerator} and
 * passed in to {@link #create(NewPreleveur, String, Clock)}.
 *
 * <p>Invariant: {@code habilitations.size() >= 1} on creation and on update (CDC §5). The aggregate
 * constructor is the backstop; canonical enforcement happens at the controller boundary via Bean
 * Validation. {@code userId} is a plain {@code String} (the iam User's ULID) to keep the module's
 * {@code allowedDependencies} free of {@code iam}.
 */
@Entity
@Table(name = "preleveurs")
@SQLDelete(
        sql =
                "UPDATE preleveurs SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
public class Preleveur {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(length = 30, nullable = false, unique = true)
    private String code;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(length = 30)
    @Nullable
    private String phone;

    @Column(length = 120)
    @Nullable
    private String email;

    @Column(name = "user_id", length = 26)
    @Nullable
    private String userId;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = Habilitation.class)
    @CollectionTable(
            name = "preleveur_habilitations",
            joinColumns = @JoinColumn(name = "preleveur_id"))
    @Column(name = "habilitation", length = 40)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 32)
    private Set<Habilitation> habilitations = new HashSet<>();

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

    protected Preleveur() {}

    /** {@code code} is server-generated ({@code CatalogCodeGenerator}) and passed in. */
    public static Preleveur create(NewPreleveur cmd, String code, Clock clock) {
        if (cmd.habilitations() == null || cmd.habilitations().isEmpty()) {
            throw new MissingHabilitationException();
        }
        var p = new Preleveur();
        p.id = PreleveurId.generate().value();
        p.code = PreleveurCode.of(code).value();
        p.name = cmd.name();
        p.phone = cmd.phone();
        p.email = cmd.email();
        p.userId = cmd.userId();
        p.habilitations = new HashSet<>(cmd.habilitations());
        var now = Instant.now(clock);
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    /**
     * Update fields; {@code null} on a primitive field means "leave unchanged". For {@code
     * habilitations}: {@code null} = no change; non-null empty = {@link
     * MissingHabilitationException}; non-null non-empty = full replace.
     */
    public void update(UpdatePreleveur cmd, Clock clock) {
        if (cmd.name() != null) this.name = cmd.name();
        if (cmd.phone() != null) this.phone = cmd.phone();
        if (cmd.email() != null) this.email = cmd.email();
        if (cmd.userId() != null) this.userId = cmd.userId();
        if (cmd.habilitations() != null) {
            if (cmd.habilitations().isEmpty()) throw new MissingHabilitationException();
            this.habilitations = new HashSet<>(cmd.habilitations());
        }
        this.updatedAt = Instant.now(clock);
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public PreleveurId id() {
        return PreleveurId.of(id);
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

    public @Nullable String phone() {
        return phone;
    }

    public @Nullable String email() {
        return email;
    }

    public @Nullable String userId() {
        return userId;
    }

    /** Returns an unmodifiable view of the habilitation set (defensive copy). */
    public Set<Habilitation> habilitations() {
        return Set.copyOf(habilitations);
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
