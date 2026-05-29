package org.sparkcorporation.lims.notification.infra;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.notification.domain.NotificationOutboxEntry;

public interface NotificationOutboxRepository
        extends JpaRepository<NotificationOutboxEntry, String> {

    /**
     * Claims up to {@code page} entries that are due now — {@code PENDING} or {@code FAILED} with
     * {@code nextAttemptAt <= now} — ordered oldest-first. The pessimistic write lock with a {@code
     * lock.timeout = -2} hint maps to PostgreSQL {@code FOR UPDATE SKIP LOCKED} so concurrent
     * pollers never block on each other under a future multi-instance deployment.
     */
    @Query(
            """
            select e from NotificationOutboxEntry e
            where e.status in (org.sparkcorporation.lims.notification.api.DeliveryStatus.PENDING,
                               org.sparkcorporation.lims.notification.api.DeliveryStatus.FAILED)
              and e.nextAttemptAt <= :now
            order by e.nextAttemptAt asc
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<NotificationOutboxEntry> claimDue(@Param("now") Instant now, Pageable page);
}
