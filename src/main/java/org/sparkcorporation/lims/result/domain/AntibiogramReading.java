package org.sparkcorporation.lims.result.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.sparkcorporation.lims.catalog.api.AntibiogramInterpreter;
import org.sparkcorporation.lims.result.api.AntibiogramReadingId;
import org.sparkcorporation.lims.shared.domain.Sensitivity;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * {@code AntibiogramReading} aggregate-internal entity per spec §4.3. {@code
 * interpretedSensitivity} is denormalized at create time via {@link AntibiogramInterpreter} so
 * later reads do not have to re-resolve the catalog antibiotic.
 */
@Entity
@Table(name = "antibiogram_readings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AntibiogramReading {

    @Id
    @Column(name = "id", length = 26, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private Result result;

    @Column(name = "antibiotic_id", length = 26, nullable = false)
    private String antibioticId;

    @Column(name = "measured_diameter", precision = 5, scale = 2, nullable = false)
    private BigDecimal measuredDiameter;

    @Column(name = "interpreted_sensitivity", length = 1, nullable = false)
    @Enumerated(EnumType.STRING)
    private Sensitivity interpretedSensitivity;

    @Column(name = "position", nullable = false)
    private int position;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 26, updatable = false)
    private @Nullable String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 26)
    private @Nullable String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    static AntibiogramReading create(Result parent, NewAntibiogramReading cmd, int position) {
        AntibiogramReading ar = new AntibiogramReading();
        ar.id = Ulid.generate();
        ar.result = parent;
        ar.antibioticId = cmd.antibioticId().value();
        ar.measuredDiameter = cmd.measuredDiameter();
        ar.position = position;
        ar.interpretedSensitivity =
                AntibiogramInterpreter.interpret(cmd.measuredDiameter(), cmd.antibiotic());
        return ar;
    }

    public AntibiogramReadingId antibiogramReadingId() {
        return new AntibiogramReadingId(id);
    }
}
