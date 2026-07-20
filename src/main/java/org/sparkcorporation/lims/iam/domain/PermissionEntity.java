package org.sparkcorporation.lims.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

/** JPA mapping for the {@code permissions} reference table. Read-only at runtime. */
@Entity
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(length = 60, nullable = false, unique = true)
    private String name;

    @Column(length = 255)
    @Nullable
    private String description;

    @Column(name = "cdc_clause", length = 20)
    @Nullable
    private String cdcClause;

    protected PermissionEntity() {}

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }
}
