package org.sparkcorporation.lims.iam.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * User aggregate. JPA entity with soft delete via Hibernate {@link SQLDelete} + {@link
 * SQLRestriction}.
 *
 * <p>Row-level history (SP8): {@code @Audited} — every insert/update/soft-delete writes a {@code
 * users_aud} row. The {@code roles} many-to-many is {@code @NotAudited}: {@code RoleEntity} is not
 * an audited entity (role membership history is out of SP8 scope), so the association must be
 * excluded or Envers fails to bootstrap.
 */
@Entity
@Audited
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(
        sql =
                "UPDATE users SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
@SQLRestriction("deleted_at IS NULL")
public class User {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(length = 160, nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", length = 160, nullable = false)
    private String fullName;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_login_at")
    @Nullable
    private Instant lastLoginAt;

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

    @ManyToMany
    @NotAudited
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    protected User() {}

    public static User create(
            String email, String fullName, String passwordHash, Set<RoleEntity> roles) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        var u = new User();
        u.id = Ulid.generate();
        u.email = email.toLowerCase();
        u.fullName = fullName.trim();
        u.passwordHash = passwordHash;
        u.enabled = true;
        u.roles = new HashSet<>(roles);
        return u;
    }

    public void changePassword(String newHash) {
        if (newHash == null || newHash.isBlank()) {
            throw new IllegalArgumentException("newHash must not be blank");
        }
        this.passwordHash = newHash;
    }

    public void recordLogin(Instant when) {
        this.lastLoginAt = when;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }

    public void update(UpdateUser cmd) {
        if (cmd.email() != null) {
            this.email = cmd.email().toLowerCase();
        }
        if (cmd.fullName() != null) {
            this.fullName = cmd.fullName().trim();
        }
        if (cmd.enabled() != null) {
            this.enabled = cmd.enabled();
        }
    }

    public void replaceRoles(Set<RoleEntity> newRoles) {
        this.roles.clear();
        this.roles.addAll(newRoles);
    }

    public boolean hasRoleNamed(String roleName) {
        return roles.stream().anyMatch(r -> r.name().equals(roleName));
    }

    public UserId id() {
        return new UserId(id);
    }

    public String rawId() {
        return id;
    }

    public String email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public boolean enabled() {
        return enabled;
    }

    public @Nullable Instant lastLoginAt() {
        return lastLoginAt;
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

    public Set<RoleEntity> roles() {
        return roles;
    }

    public int version() {
        return version;
    }
}
