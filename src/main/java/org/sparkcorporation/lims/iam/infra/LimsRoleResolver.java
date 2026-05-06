package org.sparkcorporation.lims.iam.infra;

import java.util.Set;
import java.util.stream.Collectors;

import dev.sparksam.securedx.rbac.Permission;
import dev.sparksam.securedx.rbac.RoleResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LimsRoleResolver implements RoleResolver {

    private final UserRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Set<Permission> resolvePermissions(String subject) {
        return repo.findPermissionNamesByUserId(subject).stream()
                .map(Permission::new)
                .collect(Collectors.toUnmodifiableSet());
    }
}
