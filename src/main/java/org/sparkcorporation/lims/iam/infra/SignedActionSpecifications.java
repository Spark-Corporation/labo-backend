package org.sparkcorporation.lims.iam.infra;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable {@link Specification}s over {@link SignedActionEntity} for the richer {@code GET
 * /api/v1/audit/actions} filters. AND-composes only the non-null criteria.
 *
 * <p><strong>Timestamp range</strong>: {@code timestamp} is persisted as TEXT (ISO-8601 UTC) via
 * {@link InstantIso8601Converter}. The range predicate compares the column <em>lexically</em>
 * against the ISO-8601 strings of the bounds — for fixed-length, UTC, zero-padded ISO-8601
 * instants, lexical order equals chronological order. We compare on the TEXT representation (the
 * path cast to {@link String}) rather than binding {@code Instant} bounds, because the JPA
 * converter is not guaranteed to be applied to literal bounds in a Criteria predicate, which could
 * bind a non-ISO value and break the range. The IT ({@code AuditActionsFilterIT}) is the arbiter.
 */
public final class SignedActionSpecifications {

    private SignedActionSpecifications() {}

    public static Specification<SignedActionEntity> build(
            @Nullable String subject,
            @Nullable String resource,
            @Nullable String action,
            @Nullable String fromIso,
            @Nullable String toIso) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (subject != null && !subject.isBlank()) {
                predicates.add(cb.equal(root.get("subject"), subject));
            }
            if (resource != null && !resource.isBlank()) {
                predicates.add(cb.like(root.get("resource"), "%" + resource + "%"));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            // Compare on the TEXT column (timestamp attribute cast to String) so the lexical
            // ISO-8601 range matches chronological order regardless of converter application.
            var timestamp = root.get("timestamp").as(String.class);
            if (fromIso != null && !fromIso.isBlank()) {
                predicates.add(cb.greaterThanOrEqualTo(timestamp, fromIso));
            }
            if (toIso != null && !toIso.isBlank()) {
                predicates.add(cb.lessThanOrEqualTo(timestamp, toIso));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
