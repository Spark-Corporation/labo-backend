package org.sparkcorporation.lims.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void create_normalizes_email_to_lowercase() {
        var u = User.create("Alice@Lab.TG", "Alice DOE", "{argon2id}fakehash", Set.of());
        assertThat(u.email()).isEqualTo("alice@lab.tg");
    }

    @Test
    void create_rejects_blank_email() {
        assertThatThrownBy(() -> User.create("  ", "Alice", "hash", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejects_blank_fullName() {
        assertThatThrownBy(() -> User.create("a@b.c", "  ", "hash", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejects_blank_passwordHash() {
        assertThatThrownBy(() -> User.create("a@b.c", "Alice", "  ", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void newly_created_user_is_enabled_and_not_deleted() {
        var u = User.create("a@b.c", "Alice", "hash", Set.of());
        assertThat(u.enabled()).isTrue();
        assertThat(u.deletedAt()).isNull();
    }

    @Test
    void changePassword_replaces_hash() {
        var u = User.create("a@b.c", "Alice", "old-hash", Set.of());
        u.changePassword("new-hash");
        assertThat(u.passwordHash()).isEqualTo("new-hash");
    }

    @Test
    void changePassword_rejects_blank() {
        var u = User.create("a@b.c", "Alice", "old", Set.of());
        assertThatThrownBy(() -> u.changePassword("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void disable_flips_enabled_false() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        u.disable();
        assertThat(u.enabled()).isFalse();
    }

    @Test
    void hasRoleNamed_reflects_role_set() {
        var admin = org.mockito.Mockito.mock(RoleEntity.class);
        org.mockito.Mockito.when(admin.name()).thenReturn("ADMIN");
        var u = User.create("a@b.c", "Alice", "h", Set.of(admin));
        assertThat(u.hasRoleNamed("ADMIN")).isTrue();
        assertThat(u.hasRoleNamed("TECHNICIAN")).isFalse();
    }

    @Test
    void update_changes_only_non_null_fields() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        u.update(new UpdateUser("alice@lab.tg", null, null));
        assertThat(u.email()).isEqualTo("alice@lab.tg");
        assertThat(u.fullName()).isEqualTo("Alice");
    }
}
