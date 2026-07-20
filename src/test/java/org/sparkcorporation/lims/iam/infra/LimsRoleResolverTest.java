package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import dev.sparksam.securedx.rbac.Permission;
import org.junit.jupiter.api.Test;

class LimsRoleResolverTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final LimsRoleResolver resolver = new LimsRoleResolver(repo);

    @Test
    void resolves_set_of_Permissions_for_known_subject() {
        when(repo.findPermissionNamesByUserId("01HZB0000000000000000ADMNZ"))
                .thenReturn(Set.of("patient.create", "users.manage"));

        Set<Permission> result = resolver.resolvePermissions("01HZB0000000000000000ADMNZ");
        assertThat(result)
                .extracting(Permission::key)
                .containsExactlyInAnyOrder("patient.create", "users.manage");
    }

    @Test
    void returns_empty_set_for_unknown_subject() {
        when(repo.findPermissionNamesByUserId(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Set.of());
        assertThat(resolver.resolvePermissions("01HZB0000000000000000GHSTZ")).isEmpty();
    }

    @Test
    void returns_unmodifiable_set() {
        when(repo.findPermissionNamesByUserId(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Set.of("patient.create"));
        var result = resolver.resolvePermissions("01HZB0000000000000000ADMNZ");
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> result.add(new Permission("evil")));
    }
}
