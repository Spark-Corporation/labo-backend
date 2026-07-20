package org.sparkcorporation.lims.catalog.api;

/**
 * SPI declared by {@code catalog}, implemented by {@code iam}.
 *
 * <p>Catalog uses this to verify a Préleveur's optional {@code userId} points at a real iam User
 * before persisting, so a non-existent user produces a 422 (via {@code IamUserNotFoundException} →
 * catalog problem-detail mapper) instead of a 500 (via {@code DataIntegrityViolationException},
 * which the shared advice does not map).
 *
 * <p>The SPI inverts the dependency: catalog declares the contract; iam imports {@code
 * catalog::api} to provide the implementation. This keeps catalog's {@code allowedDependencies =
 * {"shared::domain", "shared::id"}} unchanged.
 */
public interface IamUserExistenceCheck {

    /** Returns {@code true} iff an active iam User with the given id exists. */
    boolean exists(String userId);
}
