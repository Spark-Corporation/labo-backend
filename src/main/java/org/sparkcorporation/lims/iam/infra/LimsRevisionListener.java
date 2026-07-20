package org.sparkcorporation.lims.iam.infra;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Stamps each Envers revision with the acting user id. Replicates the principal-extraction rule of
 * {@link LimsAuditorAware} exactly: a {@code null} / unauthenticated / {@code anonymousUser}
 * security context yields a {@code null} user id (anonymous / system writes are recorded with no
 * actor).
 */
public class LimsRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        var rev = (LimsRevisionEntity) revisionEntity;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            rev.setUserId(null);
            return;
        }
        rev.setUserId(auth.getName());
    }
}
