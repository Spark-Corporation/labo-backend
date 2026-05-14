package org.sparkcorporation.lims.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Per-(labCode, date) invoice-number sequence counter. {@link InvoiceNumberGenerator} reads
 * pessimistic-write-locked and increments {@code lastSeq} inside its own transaction.
 *
 * <p>Spec §5.5: the strictly-monotonic guarantee per {@code (labCode, date)} is what gives the
 * generator its no-gap, no-duplicate properties. The 1.4 {@code LabFileRegistrationCounter} uses an
 * UPSERT-with-RETURNING shape; here we use {@code SELECT … FOR UPDATE} (pessimistic write) to match
 * the spec wording and keep the {@code last_seq} mutation visible to JPA dirty-checking.
 */
@Entity
@Table(name = "invoice_daily_counters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceDailyCounter {

    @EmbeddedId private CounterKey id;

    @Column(name = "last_seq", nullable = false)
    private int lastSeq;

    @Version private long version;

    public static InvoiceDailyCounter create(CounterKey key) {
        var c = new InvoiceDailyCounter();
        c.id = key;
        c.lastSeq = 0;
        return c;
    }

    /** Bump the counter and return the post-increment value. */
    public int increment() {
        this.lastSeq = this.lastSeq + 1;
        return this.lastSeq;
    }
}
