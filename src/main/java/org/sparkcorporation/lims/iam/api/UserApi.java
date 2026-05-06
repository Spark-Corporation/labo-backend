package org.sparkcorporation.lims.iam.api;

import java.util.Optional;

/** Cross-module read-only iface. Other modules import {@code iam.api} only. */
public interface UserApi {

    Optional<UserSummary> findById(UserId id);

    UserSummary requireById(UserId id);

    boolean existsById(UserId id);
}
