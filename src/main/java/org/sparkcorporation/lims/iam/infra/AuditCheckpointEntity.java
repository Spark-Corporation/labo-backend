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

/**
 * Append-only persisted mirror of secured-x {@code AuditCheckpoint}. DB RULEs block update/delete.
 */
@Entity
@Table(name = "audit_checkpoints")
@Getter
@Setter
@NoArgsConstructor
public class AuditCheckpointEntity {

    @Id
    @Column(name = "checkpoint_id", length = 36, nullable = false)
    private String checkpointId;

    @Column(name = "global_sequence", nullable = false)
    private long globalSequence;

    @Convert(converter = InstantIso8601Converter.class)
    @Column(name = "timestamp", columnDefinition = "text", nullable = false)
    private Instant timestamp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chain_heads", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> chainHeads;

    @Column(name = "merkle_root", length = 64, nullable = false)
    private String merkleRoot;

    @Column(name = "signature", columnDefinition = "text", nullable = false)
    private String signature;

    @Column(name = "key_id", length = 36, nullable = false)
    private String keyId;
}
