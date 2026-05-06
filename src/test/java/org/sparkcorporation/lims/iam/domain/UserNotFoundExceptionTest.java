package org.sparkcorporation.lims.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.iam.api.UserId;

class UserNotFoundExceptionTest {

    @Test
    void is_an_EntityNotFoundException_so_ProblemDetailAdvice_maps_to_404() {
        assertThat(new UserNotFoundException(UserId.next()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void message_includes_id() {
        var id = UserId.next();
        assertThat(new UserNotFoundException(id).getMessage()).contains(id.value());
    }
}
