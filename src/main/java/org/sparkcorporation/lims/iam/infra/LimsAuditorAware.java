package org.sparkcorporation.lims.iam.infra;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("auditorAware")
public class LimsAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.of(auth.getName());
    }
}
