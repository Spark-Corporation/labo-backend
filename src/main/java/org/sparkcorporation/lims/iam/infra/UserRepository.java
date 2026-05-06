package org.sparkcorporation.lims.iam.infra;

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

    @Query(
            """
            SELECT p.name FROM User u JOIN u.roles r JOIN r.permissions p
            WHERE u.id = :userId
            """)
    Set<String> findPermissionNamesByUserId(@Param("userId") String userId);

    @Query(
            """
            SELECT u FROM User u
            WHERE (:q IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                              OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:roleName IS NULL OR EXISTS (
                    SELECT 1 FROM u.roles r WHERE r.name = :roleName))
            """)
    Page<User> search(
            @Param("q") @org.springframework.lang.Nullable String q,
            @Param("roleName") @org.springframework.lang.Nullable String roleName,
            Pageable pageable);
}
