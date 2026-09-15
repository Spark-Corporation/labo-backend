package org.sparkcorporation.lims.shared.web;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
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

    /** Illustrative access-token lifetime (seconds) shown in the login response example. */
    private static final int EXAMPLE_EXPIRES_IN_SECONDS = 3600;

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

                                        **Authentication.** Obtain a token from `POST /api/v1/auth/login` \
                                        with body `{"provider":"local","payload":\
                                        {"username":"…","password":"…"}}`. It returns \
                                        `accessToken`, `refreshToken`, `expiresIn` and `subject`. \
                                        Refresh via `POST /api/v1/auth/refresh` with `{"refreshToken":"…"}`\
                                        , and end the session with `POST /api/v1/auth/logout`. These come \
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
                                                        "Access token from POST"
                                                                + " /api/v1/auth/login.")))
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
     * secured-x's {@code AuthController} lives outside our source tree, so its request/response
     * DTOs (some of them {@code Map<String, Object>} or {@code Object}) cannot be annotated with
     * {@code @Schema} the way our own controllers are. This customizer rewrites the three auth
     * operations' summaries, descriptions and body schemas by hand so the document reads as
     * concretely as our own endpoints instead of showing generic maps/objects.
     */
    @Bean
    public OperationCustomizer authEndpointOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (!AUTH_CONTROLLER.equals(handlerMethod.getMethod().getDeclaringClass().getName())) {
                return operation;
            }
            switch (handlerMethod.getMethod().getName()) {
                case "login" -> describeLogin(operation);
                case "refresh" -> describeRefresh(operation);
                case "logout" -> describeLogout(operation);
                default -> {
                    // no other handlers on AuthController
                }
            }
            return operation;
        };
    }

    private static final String AUTH_CONTROLLER =
            "dev.sparksam.securedx.starter.controller.AuthController";

    private static void describeLogin(Operation operation) {
        operation.setSummary("Log in");
        operation.setDescription(
                "Authenticates against the given provider and issues an access/refresh token "
                        + "pair. Only the \"local\" provider is enabled; its payload takes "
                        + "username and password.");

        var payloadSchema =
                new ObjectSchema()
                        .description(
                                "Credentials for the given provider; shape is provider-specific.")
                        .addProperty(
                                "username",
                                new StringSchema()
                                        .description("Local account username.")
                                        .example("jdoe"))
                        .addProperty(
                                "password",
                                new StringSchema()
                                        .description("Local account password.")
                                        .example("changeme"));
        var requestSchema =
                new ObjectSchema()
                        .addProperty(
                                "provider",
                                new StringSchema()
                                        .description(
                                                "Auth provider to use; only \"local\" is enabled.")
                                        .example("local"))
                        .addProperty("payload", payloadSchema);
        var requestExample = new LinkedHashMap<String, Object>();
        requestExample.put("provider", "local");
        var payloadExample = new LinkedHashMap<String, Object>();
        payloadExample.put("username", "jdoe");
        payloadExample.put("password", "changeme");
        requestExample.put("payload", payloadExample);
        replaceRequestBody(operation, requestSchema, requestExample);

        var responseSchema =
                new ObjectSchema()
                        .description("Issued token pair.")
                        .addProperty(
                                "accessToken",
                                new StringSchema()
                                        .description(
                                                "Short-lived JWT; send as \"Authorization: Bearer"
                                                        + " <accessToken>\" on subsequent"
                                                        + " requests."))
                        .addProperty(
                                "refreshToken",
                                new StringSchema()
                                        .description(
                                                "Long-lived token; exchange it for a new access"
                                                        + " token via POST /api/v1/auth/refresh."))
                        .addProperty(
                                "expiresIn",
                                new IntegerSchema()
                                        .format("int64")
                                        .description("Access token lifetime, in seconds."))
                        .addProperty(
                                "subject",
                                new StringSchema().description("Authenticated subject (user id)."));
        var responseExample = new LinkedHashMap<String, Object>();
        responseExample.put("accessToken", "eyJhbGciOi...");
        responseExample.put("refreshToken", "eyJhbGciOi...");
        responseExample.put("expiresIn", EXAMPLE_EXPIRES_IN_SECONDS);
        responseExample.put("subject", "3f7e2c1a-4b5d-4e6f-8a9b-0c1d2e3f4a5b");
        replaceResponseBodies(operation, responseSchema, responseExample);
    }

    private static void describeRefresh(Operation operation) {
        operation.setSummary("Refresh an access token");
        operation.setDescription(
                "Exchanges a still-valid refresh token for a new access/refresh token pair.");

        var requestSchema =
                new ObjectSchema()
                        .addProperty(
                                "refreshToken",
                                new StringSchema()
                                        .description(
                                                "Refresh token previously issued by POST"
                                                        + " /api/v1/auth/login or POST"
                                                        + " /api/v1/auth/refresh.")
                                        .example("eyJhbGciOi..."));
        replaceRequestBody(operation, requestSchema, null);
    }

    private static void describeLogout(Operation operation) {
        operation.setSummary("Log out");
        operation.setDescription(
                "Invalidates the caller's current session server-side. Takes no request body.");
    }

    /** Overwrites every media type's schema (and example, if given) on the request body. */
    private static void replaceRequestBody(Operation operation, Schema<?> schema, Object example) {
        if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
            return;
        }
        operation
                .getRequestBody()
                .getContent()
                .values()
                .forEach(
                        mediaType -> {
                            mediaType.setSchema(schema);
                            if (example != null) {
                                mediaType.setExample(example);
                            }
                        });
    }

    /** Overwrites every declared response's media-type schema and example, where one has a body. */
    private static void replaceResponseBodies(
            Operation operation, Schema<?> schema, Object example) {
        if (operation.getResponses() == null) {
            return;
        }
        operation
                .getResponses()
                .values()
                .forEach(
                        response -> {
                            if (response.getContent() == null) {
                                return;
                            }
                            response.getContent()
                                    .values()
                                    .forEach(
                                            mediaType -> {
                                                mediaType.setSchema(schema);
                                                mediaType.setExample(example);
                                            });
                        });
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
