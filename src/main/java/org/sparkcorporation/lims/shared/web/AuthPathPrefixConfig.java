package org.sparkcorporation.lims.shared.web;

import dev.sparksam.securedx.starter.controller.AuthController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves secured-x's {@code /auth/login}, {@code /auth/refresh} and {@code /auth/logout} under
 * {@code /api/v1/auth/**}, so the whole staff API sits behind one {@code /api/v1} prefix.
 *
 * <p>{@link AuthController} lives in the starter jar and hard-codes
 * {@code @RequestMapping("/auth")} — there is no property to move it. Prefixing it here, scoped to
 * that one handler type, is the only way to relocate it without forking the library; none of our
 * own controllers are affected.
 *
 * <p><b>Must stay in sync with {@code application.yml}</b>: {@code secured-x.security.public-paths}
 * ({@code /api/v1/auth/**}) and {@code secured-x.signing.filter.exclude-paths} ({@code
 * /api/v1/auth}). Both are matched against the real request path, so if either still names {@code
 * /auth}, login silently becomes a protected route and every call to it returns 401.
 */
@Configuration
public class AuthPathPrefixConfig implements WebMvcConfigurer {

    static final String API_PREFIX = "/api/v1";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                API_PREFIX, HandlerTypePredicate.forAssignableType(AuthController.class));
    }
}
