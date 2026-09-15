package org.sparkcorporation.lims.iam.infra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.iam.domain.User;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByIdIn(Collection<String> ids);

    @Query(
            """
            SELECT p.name FROM User u JOIN u.roles r JOIN r.permissions p
            WHERE u.id = :userId
            """)
    Set<String> findPermissionNamesByUserId(@Param("userId") String userId);

    /**
     * Both filters are optional; a null means "no filter".
     *
     * <p>The explicit {@code CAST(... AS String)} around every use of the two parameters is load
     * bearing, not decoration. With a bare {@code :q}, Hibernate has no type to infer when the
     * bound value is null and falls back to a binary type, so PostgreSQL receives {@code
     * lower(bytea)} and rejects the statement at parse time — {@code function lower(bytea) does not
     * exist} — failing the whole unfiltered listing, not just the LIKE branch. The cast pins the
     * parameter to varchar so a null binds as a null varchar and the {@code IS NULL} guard
     * short-circuits as intended.
     */
    @Query(
            """
            SELECT u FROM User u
            WHERE (CAST(:q AS String) IS NULL
                      OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%'))
                      OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')))
              AND (CAST(:roleName AS String) IS NULL OR EXISTS (
                    SELECT 1 FROM u.roles r WHERE r.name = CAST(:roleName AS String)))
            """)
    Page<User> search(
            @Param("q") @org.springframework.lang.Nullable String q,
            @Param("roleName") @org.springframework.lang.Nullable String roleName,
            Pageable pageable);
}
