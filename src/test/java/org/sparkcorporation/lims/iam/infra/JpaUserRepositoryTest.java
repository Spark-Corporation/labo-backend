package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.iam.domain.User;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaUserRepositoryTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;

    @Test
    void save_and_find_by_email_round_trip() {
        var u = User.create("Alice@Lab.tg", "Alice DOE", "{argon2id}fakehash", Set.of());
        userRepo.save(u);
        var found = userRepo.findByEmailIgnoreCase("alice@lab.tg");
        assertThat(found).isPresent();
        assertThat(found.get().fullName()).isEqualTo("Alice DOE");
    }

    @Test
    void duplicate_email_violates_unique_constraint() {
        userRepo.saveAndFlush(User.create("a@b.c", "Alice", "h", Set.of()));
        assertThatThrownBy(() -> userRepo.saveAndFlush(User.create("a@b.c", "Bob", "h", Set.of())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void soft_delete_hides_from_findById() {
        var u = userRepo.save(User.create("a@b.c", "Alice", "h", Set.of()));
        userRepo.deleteById(u.id().value());
        userRepo.flush();
        assertThat(userRepo.findById(u.id().value())).isEmpty();
    }

    @Test
    void findPermissionNamesByUserId_returns_seeded_admin_perms_for_admin() {
        var admin = userRepo.findByEmailIgnoreCase("admin@test.local").orElseThrow();
        var perms = userRepo.findPermissionNamesByUserId(admin.id().value());
        assertThat(perms).contains("users.manage", "patient.create", "audit.read");
    }

    @Test
    void search_filters_by_role_and_query() {
        var users =
                userRepo.search(
                        "test",
                        "TECHNICIAN",
                        org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(users.getContent())
                .extracting(User::email)
                .anyMatch(e -> e.contains("technician") || e.contains("tech"));
    }
}
