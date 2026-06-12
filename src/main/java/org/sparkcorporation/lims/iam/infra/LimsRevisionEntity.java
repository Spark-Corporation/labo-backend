package org.sparkcorporation.lims.iam.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.jspecify.annotations.Nullable;

/**
 * Envers revision entity for the LIMS row-level history. Adds a {@code user_id} column to the
 * default {@code revinfo} table, populated by {@link LimsRevisionListener} from the security
 * context (null for anonymous / system writes). One {@code revinfo} row is written per transaction
 * that mutates an {@code @Audited} entity; every {@code *_aud} row references it by {@code rev}.
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(LimsRevisionListener.class)
public class LimsRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revinfo_seq")
    @SequenceGenerator(name = "revinfo_seq", sequenceName = "revinfo_seq", allocationSize = 1)
    @RevisionNumber
    @Column(name = "rev", nullable = false)
    private int id;

    @RevisionTimestamp
    @Column(name = "revtstmp", nullable = false)
    private long timestamp;

    @Column(name = "user_id", length = 26)
    private @Nullable String userId;

    public int getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public @Nullable String getUserId() {
        return userId;
    }

    public void setUserId(@Nullable String userId) {
        this.userId = userId;
    }
}
