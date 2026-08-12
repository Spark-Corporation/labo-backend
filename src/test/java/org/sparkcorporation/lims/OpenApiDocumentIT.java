package org.sparkcorporation.lims;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.sparksam.securedx.rbac.RequiresPermission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;

/**
 * Contract test for the generated OpenAPI document (spec §5 of {@code
 * 2026-08-04-openapi-springdoc-adoption-design.md}).
 *
 * <p>This is the "first failing test" for the springdoc adoption: before the dependency is on the
 * classpath {@code /v3/api-docs} 404s and every assertion here fails.
 *
 * <p>The point is not that the document merely exists — it is that it stays <em>complete</em>. The
 * coverage and permission assertions derive their expectations from the running application's own
 * handler mappings rather than from a hard-coded count, so a controller added later without
 * reaching the document fails the build instead of silently going undocumented.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocumentIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    /**
     * Qualified explicitly: the actuator contributes a second {@code RequestMappingHandlerMapping}
     * ({@code controllerEndpointHandlerMapping}) and we only want the application's own mappings.
     */
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    private JsonNode fetchApiDocs() {
        var client = RestClient.create("http://localhost:" + port);
        var response = client.get().uri("/v3/api-docs").retrieve().toEntity(JsonNode.class);
        assertThat(response.getStatusCode())
                .as("/v3/api-docs must be reachable without authentication")
                .isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    /**
     * Every {@code /api/v1/**} handler in the application must appear in the document. Portal
     * endpoints are included: they are part of the published contract even though they live in a
     * separate auth realm.
     */
    @Test
    void document_covers_every_application_endpoint() {
        var expected = applicationEndpoints();
        var documented = documentedEndpoints(fetchApiDocs());

        assertThat(expected).as("sanity: the app must expose endpoints to document").isNotEmpty();
        assertThat(documented)
                .as("every /api/v1 endpoint must appear in the OpenAPI document")
                .containsAll(expected);
    }

    /**
     * Writes the document to {@code target/openapi.json} — the hand-off artefact for the frontend
     * team (spec §4.4). Produced here rather than by {@code springdoc-openapi-maven-plugin} so the
     * published file is byte-for-byte the one the assertions above just validated, and so the build
     * needs no second application boot.
     */
    @Test
    void document_is_published_as_a_build_artefact() throws Exception {
        var target = Path.of("target", "openapi.json");
        Files.writeString(target, fetchApiDocs().toPrettyString());

        assertThat(target).isNotEmptyFile();
    }

    /**
     * Tier-2 of the annotation strategy: the {@code OperationCustomizer} must project each
     * handler's {@code @RequiresPermission} into an {@code x-lims-permissions} vendor extension, so
     * the frontend can drive its UI off permissions straight from the document.
     */
    @Test
    void every_permission_gated_operation_publishes_its_permissions() {
        var doc = fetchApiDocs();
        var missing = new ArrayList<String>();

        handlerMapping
                .getHandlerMethods()
                .forEach(
                        (info, method) -> {
                            var required = requiredPermissions(method);
                            if (required.isEmpty()) {
                                return;
                            }
                            for (var key : endpointKeys(info)) {
                                missing.addAll(mismatch(doc, key, required));
                            }
                        });

        assertThat(missing)
                .as("operations whose @RequiresPermission did not reach the document")
                .isEmpty();
    }

    /** Empty when the operation publishes exactly the permissions its handler requires. */
    private static List<String> mismatch(JsonNode doc, String key, List<String> required) {
        var op = operationAt(doc, key);
        if (op == null) {
            return List.of(key + " → absent from the document");
        }
        var published = new ArrayList<String>();
        op.path("x-lims-permissions").forEach(node -> published.add(node.asString()));
        return published.equals(required)
                ? List.of()
                : List.of(key + " → expected " + required + " but document had " + published);
    }

    // ---- helpers -------------------------------------------------------------------------

    private static List<String> requiredPermissions(HandlerMethod method) {
        var ann = method.getMethodAnnotation(RequiresPermission.class);
        return ann == null ? List.of() : List.of(ann.value());
    }

    /** "GET /api/v1/patients" style keys for every application handler. */
    private List<String> applicationEndpoints() {
        var keys = new ArrayList<String>();
        handlerMapping
                .getHandlerMethods()
                .forEach((info, method) -> keys.addAll(endpointKeys(info)));
        return keys.stream().distinct().sorted().toList();
    }

    private static List<String> endpointKeys(RequestMappingInfo info) {
        var patterns = info.getPathPatternsCondition();
        if (patterns == null) {
            return List.of();
        }
        var keys = new ArrayList<String>();
        for (var pattern : patterns.getPatternValues()) {
            if (!pattern.startsWith("/api/v1")) {
                continue;
            }
            for (var method : info.getMethodsCondition().getMethods()) {
                keys.add(method.name() + " " + pattern);
            }
        }
        return keys;
    }

    /** Flattens the document's {@code paths} object into the same "VERB /path" key space. */
    private static List<String> documentedEndpoints(JsonNode doc) {
        var keys = new ArrayList<String>();
        var paths = doc.path("paths");
        for (var path : paths.propertyNames()) {
            for (var verb : paths.path(path).propertyNames()) {
                keys.add(verb.toUpperCase(Locale.ROOT) + " " + path);
            }
        }
        return keys;
    }

    private static JsonNode operationAt(JsonNode doc, String key) {
        var split = key.split(" ", 2);
        var op = doc.path("paths").path(split[1]).path(split[0].toLowerCase(Locale.ROOT));
        return op.isMissingNode() ? null : op;
    }
}
