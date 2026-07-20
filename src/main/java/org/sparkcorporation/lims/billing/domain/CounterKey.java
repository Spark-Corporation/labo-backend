package org.sparkcorporation.lims.billing.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for {@link InvoiceDailyCounter} — pairs the lab-code with the calendar
 * date. Embedded into the entity via {@code @EmbeddedId}.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CounterKey implements Serializable {

    @Column(name = "lab_code", length = 3, nullable = false)
    private String labCode;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    public CounterKey(String labCode, LocalDate date) {
        this.labCode = Objects.requireNonNull(labCode, "labCode");
        this.date = Objects.requireNonNull(date, "date");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CounterKey other)) {
            return false;
        }
        return Objects.equals(labCode, other.labCode) && Objects.equals(date, other.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labCode, date);
    }
}
