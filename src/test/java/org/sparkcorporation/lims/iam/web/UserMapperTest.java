package org.sparkcorporation.lims.iam.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.iam.domain.User;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toResponse_maps_user_aggregate_to_dto() {
        var u = User.create("alice@lab.tg", "Alice DOE", "{argon2id}h", Set.of());
        var resp = mapper.toResponse(u);
        assertThat(resp.id()).isEqualTo(u.id().value());
        assertThat(resp.email()).isEqualTo("alice@lab.tg");
        assertThat(resp.fullName()).isEqualTo("Alice DOE");
        assertThat(resp.enabled()).isTrue();
        assertThat(resp.roles()).isEmpty();
        assertThat(resp.version()).isZero();
    }

    @Test
    void toUpdateCommand_passes_through_nullables() {
        var req = new UpdateUserRequest("new@email.tg", null, false);
        var cmd = mapper.toUpdateCommand(req);
        assertThat(cmd.email()).isEqualTo("new@email.tg");
        assertThat(cmd.fullName()).isNull();
        assertThat(cmd.enabled()).isFalse();
    }
}
