package org.sparkcorporation.lims.iam.infra;

import java.time.Instant;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

/**
 * Append-only JPA mirror of secured-x's {@code SignedAction} (one tamper-evident hash-chain link
 * per user action). The DB enforces immutability via {@code no_update_signed_actions} / {@code
 * no_delete_signed_actions} rules (changeset 202605281405). No {@code @Version} — append-only, and
 * the DB rule blocks updates anyway.
 */
@Entity
@Table(name = "signed_actions")
@Getter
@Setter
@NoArgsConstructor
public class SignedActionEntity {

    @Id
    @Column(name = "action_id", length = 36, nullable = false)
    private String actionId;

    @Column(name = "subject", length = 26, nullable = false)
    private String subject;

    @Column(name = "action", length = 120, nullable = false)
    private String action;

    @Column(name = "resource", length = 255)
    private @Nullable String resource;

    @Convert(converter = InstantIso8601Converter.class)
    @Column(name = "timestamp", columnDefinition = "text", nullable = false)
    private Instant timestamp;

    @Column(name = "correlation_id", length = 64)
    private @Nullable String correlationId;

    @Column(name = "key_id", length = 36, nullable = false)
    private String keyId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private @Nullable Map<String, Object> metadata;

    @Column(name = "previous_hash", length = 64)
    private @Nullable String previousHash;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "signature", columnDefinition = "text", nullable = false)
    private String signature;
}
