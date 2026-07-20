package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.api.ResultSummary;
import org.sparkcorporation.lims.result.domain.FreeFormResult;
import org.sparkcorporation.lims.result.domain.IntegerResult;
import org.sparkcorporation.lims.result.domain.NumericResult;
import org.sparkcorporation.lims.result.domain.TextResult;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Unit tests for {@link ResultMapper} covering all 5 {@link PatchLineRequest.ValueUnion} arms and
 * the null-referenceIntervalId branch in {@link ResultMapper#toLineResponse}.
 */
class ResultMapperTest {

    // ---- toDomainValue ----

    @Test
    void toDomainValue_integer_variant_produces_IntegerResult() {
        var union = new PatchLineRequest.ValueUnion.IntegerVariant(42L);
        var result = ResultMapper.toDomainValue(union);
        assertThat(result).isInstanceOf(IntegerResult.class);
        assertThat(((IntegerResult) result).value()).isEqualTo(42L);
    }

    @Test
    void toDomainValue_real_variant_produces_NumericResult() {
        var union = new PatchLineRequest.ValueUnion.RealVariant("3.14");
        var result = ResultMapper.toDomainValue(union);
        assertThat(result).isInstanceOf(NumericResult.class);
        assertThat(((NumericResult) result).value()).isEqualByComparingTo("3.14");
    }

    @Test
    void toDomainValue_text_variant_produces_TextResult() {
        var union = new PatchLineRequest.ValueUnion.TextVariant("Positif");
        var result = ResultMapper.toDomainValue(union);
        assertThat(result).isInstanceOf(TextResult.class);
        assertThat(((TextResult) result).value()).isEqualTo("Positif");
    }

    @Test
    void toDomainValue_free_variant_produces_FreeFormResult() {
        var union = new PatchLineRequest.ValueUnion.FreeVariant("free text");
        var result = ResultMapper.toDomainValue(union);
        assertThat(result).isInstanceOf(FreeFormResult.class);
        assertThat(((FreeFormResult) result).value()).isEqualTo("free text");
    }

    // ---- toLineResponse ----

    @Test
    void toLineResponse_null_reference_interval_id_leaves_field_null() {
        var line = lineSummary(null);
        var resp = ResultMapper.toLineResponse(line);
        assertThat(resp.referenceIntervalId()).isNull();
    }

    @Test
    void toLineResponse_non_null_reference_interval_id_extracts_value() {
        var refId = Ulid.generate();
        var line = lineSummary(new ReferenceIntervalId(refId));
        var resp = ResultMapper.toLineResponse(line);
        assertThat(resp.referenceIntervalId()).isEqualTo(refId);
    }

    // ---- toResponse ----

    @Test
    void toResponse_maps_summary_to_response() {
        var summary = minimalSummary();
        var resp = ResultMapper.toResponse(summary);
        assertThat(resp.id()).isEqualTo(summary.id().value());
        assertThat(resp.state()).isEqualTo("DRAFT");
        assertThat(resp.lines()).isEmpty();
        assertThat(resp.antibiogramReadings()).isEmpty();
    }

    // ---- helpers ----

    private static ResultSummary.LineSummary lineSummary(ReferenceIntervalId refId) {
        return new ResultSummary.LineSummary(
                new ResultLineId(Ulid.generate()),
                new AnalysisId(Ulid.generate()),
                "GLU",
                0,
                ResultType.REAL,
                null,
                BigDecimal.valueOf(5.0),
                null,
                "mmol/L",
                2,
                null,
                null,
                null,
                refId,
                false,
                null);
    }

    private static ResultSummary minimalSummary() {
        return new ResultSummary(
                new ResultId(Ulid.generate()),
                new LabFileId(Ulid.generate()),
                new AnalysisRequestId(Ulid.generate()),
                new AnalysisId(Ulid.generate()),
                "GLU",
                ResultState.DRAFT,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                Instant.now(),
                null);
    }
}
