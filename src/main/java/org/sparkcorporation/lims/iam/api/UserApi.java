package org.sparkcorporation.lims.iam.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Cross-module read-only iface. Other modules import {@code iam.api} only. */
public interface UserApi {

    Optional<UserSummary> findById(UserId id);

    UserSummary requireById(UserId id);

    boolean existsById(UserId id);

    /**
     * Returns a map keyed by {@link UserId}, containing only entries for which a user exists.
     * Absent ids are excluded from the result (no {@code null} values). Added in 1.7.a for cashbox
     * journal-row enrichment.
     */
    Map<UserId, UserSummary> findAllByIds(Set<UserId> ids);
}
