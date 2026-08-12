package org.sparkcorporation.lims.shared.web;

import java.util.Arrays;
import java.util.List;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document assembly (spec {@code 2026-08-04-openapi-springdoc-adoption-design.md}).
 *
 * <p>springdoc derives paths, verbs, schemas and validation constraints on its own. This class adds
 * the two things it cannot infer: the bearer security scheme, and the per-operation permission that
 * the frontend uses to decide whether to render a control at all.
 *
 * <p>Beans here are inert when {@code springdoc.api-docs.enabled=false} (the default outside
 * dev/test) — springdoc simply never invokes the customizer.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI limsOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("LIMS Backend API")
                                .version("v1")
                                .description(
                                        """
                                        Laboratory Information Management System — REST API.

                                        **Authentication.** Obtain a token from `POST /auth/login` \
                                        with body `{"provider":"local","payload":\
                                        {"username":"…","password":"…"}}`. It returns \
                                        `accessToken`, `refreshToken`, `expiresIn` and `subject`. \
                                        Refresh via `POST /auth/refresh` with `{"refreshToken":"…"}`\
                                        , and end the session with `POST /auth/logout`. These come \
                                        from the secured-x starter, so their request bodies are \
                                        typed loosely below — the shapes given here are \
                                        authoritative.

                                        **Authorization.** Each operation carries an \
                                        `x-lims-permissions` extension naming the permission(s) its \
                                        caller must hold. A valid token without the permission gets \
                                        a 403.

                                        **Patient portal.** `/api/v1/portal/**` is a separate auth \
                                        realm with its own short-lived token from \
                                        `POST /api/v1/portal/login`; staff tokens do not work there.

                                        **Errors.** Most failures are RFC 7807 `ProblemDetail` \
                                        documents: body validation is 422 with an `errors[]` array, \
                                        malformed query parameters are 400, unknown ids are 404. \
                                        Two exceptions produced by the security layer do **not** use \
                                        that envelope — 401 returns an empty body, and 403 returns \
                                        `{"error":"authorization_denied","message":"…","status":403}`\
                                        . The `traceId` property currently always reads `unknown`; \
                                        do not build support tooling on it yet.

                                        **Known issue.** An out-of-range `size` returns 500 rather \
                                        than 400 on paginated endpoints whose controller lacks \
                                        `@Validated` (e.g. `/api/v1/patients`, `/api/v1/admin/users`)\
                                        . Clamp `size` client-side.
                                        """))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        BEARER_SCHEME,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "Access token from POST /auth/login.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /**
     * Projects each handler's {@code @RequiresPermission} into the document.
     *
     * <p>Deliberately reflection-driven rather than hand-written per endpoint: the annotation is
     * already the enforcement mechanism, so reading it here means the published permission and the
     * enforced permission cannot drift. {@code OpenApiDocumentIT} asserts exactly that.
     */
    @Bean
    public OperationCustomizer permissionOperationCustomizer() {
        return (operation, handlerMethod) -> {
            operation.setTags(List.of(moduleTag(handlerMethod.getBeanType().getPackageName())));

            var annotation = handlerMethod.getMethodAnnotation(RequiresPermission.class);
            if (annotation == null) {
                return operation;
            }
            List<String> permissions = Arrays.asList(annotation.value());
            operation.addExtension("x-lims-permissions", permissions);

            var note =
                    permissions.size() == 1
                            ? "Requires permission: " + permissions.get(0)
                            : "Requires permissions ("
                                    + annotation.match().name()
                                    + "): "
                                    + String.join(", ", permissions);
            operation.setDescription(
                    operation.getDescription() == null || operation.getDescription().isBlank()
                            ? note
                            : operation.getDescription() + "\n\n" + note);
            return operation;
        };
    }

    /**
     * Groups operations by Modulith module rather than by controller class.
     *
     * <p>springdoc's default is one tag per controller, which yields 60 groups named {@code
     * analysis-admin-controller} and the like. Deriving the tag from the owning package collapses
     * those onto the 11 modules the API is actually organised around — again without touching a
     * single controller.
     */
    private static String moduleTag(String packageName) {
        var marker = ".lims.";
        var index = packageName.indexOf(marker);
        if (index < 0) {
            return "authentication"; // secured-x's AuthController, outside our package tree
        }
        var remainder = packageName.substring(index + marker.length());
        var dot = remainder.indexOf('.');
        return dot < 0 ? remainder : remainder.substring(0, dot);
    }
}
