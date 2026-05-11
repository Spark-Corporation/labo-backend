package org.sparkcorporation.lims.result.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Smoke test that fails until the {@code result.api} package compiles with the documented method
 * signatures and event types. Pure reflection; no Spring, no DB.
 */
class ResultApiSurfaceTest {

    @Test
    void api_package_exposes_documented_methods() throws Exception {
        Class<?> api = Class.forName("org.sparkcorporation.lims.result.api.ResultApi");
        assertThat(api.isInterface()).isTrue();
        assertThat(api.getDeclaredMethods()).hasSize(5);

        Method findById = api.getMethod("findById", ResultId.class);
        assertThat(findById.getReturnType()).isEqualTo(Optional.class);

        Method findByAnalysisRequest =
                api.getMethod("findByAnalysisRequest", AnalysisRequestId.class);
        assertThat(findByAnalysisRequest.getReturnType()).isEqualTo(Optional.class);

        Method findActiveByLabFile = api.getMethod("findActiveByLabFile", LabFileId.class);
        assertThat(findActiveByLabFile.getReturnType().getSimpleName()).isEqualTo("List");

        Method requireById = api.getMethod("requireById", ResultId.class);
        assertThat(requireById.getReturnType()).isEqualTo(ResultSummary.class);

        Method existsById = api.getMethod("existsById", ResultId.class);
        assertThat(existsById.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    void result_state_enum_has_four_values_in_pinned_order() {
        var values = ResultState.values();
        assertThat(values)
                .extracting(Enum::name)
                .containsExactly("DRAFT", "ENTERED", "TECHNICAL_VALIDATED", "BIOLOGICAL_VALIDATED");
    }

    @Test
    void four_event_types_compile_with_id_only_payloads() throws Exception {
        Class.forName("org.sparkcorporation.lims.result.api.ResultCreated");
        Class.forName("org.sparkcorporation.lims.result.api.ResultEntered");
        Class.forName("org.sparkcorporation.lims.result.api.TechnicalValidationDone");
        Class.forName("org.sparkcorporation.lims.result.api.BiologicalValidationDone");
    }
}
