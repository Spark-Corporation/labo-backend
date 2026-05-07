package org.sparkcorporation.lims.iam.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.api.IamUserExistenceCheck;

/**
 * iam-side implementation of the {@link IamUserExistenceCheck} SPI declared by {@code
 * catalog::api}.
 *
 * <p>Catalog uses this to verify a Préleveur's optional {@code userId} points at a real iam User
 * before persisting, so a non-existent user produces a 422 (via {@code IamUserNotFoundException}
 * mapped by the catalog problem-detail advice) instead of a 500 from a {@code
 * DataIntegrityViolationException}.
 *
 * <p>The dependency is inverted: catalog declares the interface; iam imports {@code catalog::api}
 * to provide this implementation. See {@code iam/package-info.java} which extends {@code
 * allowedDependencies} with {@code "catalog::api"} for exactly this single import.
 */
@Component
@RequiredArgsConstructor
class IamUserExistenceCheckImpl implements IamUserExistenceCheck {

    private final UserRepository users;

    @Override
    public boolean exists(String userId) {
        return users.existsById(userId);
    }
}
