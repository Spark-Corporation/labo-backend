package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IamUserExistenceCheckImplTest {

    @Mock UserRepository users;

    @InjectMocks IamUserExistenceCheckImpl impl;

    @Test
    void exists_returns_true_when_user_present() {
        when(users.existsById("01HZX0000000000000000USER")).thenReturn(true);
        assertThat(impl.exists("01HZX0000000000000000USER")).isTrue();
    }

    @Test
    void exists_returns_false_when_user_absent() {
        when(users.existsById("01HZX0000000000000MISSING")).thenReturn(false);
        assertThat(impl.exists("01HZX0000000000000MISSING")).isFalse();
    }
}
