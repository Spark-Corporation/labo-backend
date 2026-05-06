package org.sparkcorporation.lims.iam.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.iam.api.Role;

/**
 * JPA mapping for the {@code roles} reference table. Read-only at runtime — seeded via Liquibase.
 */
@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(length = 30, nullable = false, unique = true)
    private String name;

    @Column(length = 255)
    @Nullable
    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @ManyToMany
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<PermissionEntity> permissions = new HashSet<>();

    protected RoleEntity() {}

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Role asEnum() {
        return Role.valueOf(name);
    }

    public Set<PermissionEntity> permissions() {
        return permissions;
    }
}
