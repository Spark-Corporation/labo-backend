package org.sparkcorporation.lims.result.infra.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.api.ResultSummary;
import org.sparkcorporation.lims.shared.id.Ulid;

class CompteRenduContentDigestTest {

    private final ResultApi resultApi = mock(ResultApi.class);
    private final DefaultCompteRenduApi api =
            new DefaultCompteRenduApi(
                    mock(ResultReportModelAssembler.class),
                    mock(ResultReportPdfDocument.class),
                    mock(PdfRenderingService.class),
                    resultApi);

    private static final LabFileId LAB_FILE = new LabFileId("01HZXCD0000000000000000002");
    private static final Instant T = Instant.parse("2026-05-28T08:00:00Z");
    // Fixed signer ULID so the digest input only varies on the content under test (not the signer).
    private static final String BIOLOGIST = "01HZXCD0000000000000000003";

    @Test
    void digest_is_64_char_lowercase_hex() {
        when(resultApi.findActiveByLabFile(LAB_FILE)).thenReturn(twoResults("1.234"));

        var digest = api.contentDigest(LAB_FILE);

        assertThat(digest).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void digest_is_stable_across_calls_for_same_content() {
        when(resultApi.findActiveByLabFile(LAB_FILE)).thenReturn(twoResults("1.234"));

        var first = api.contentDigest(LAB_FILE);
        var second = api.contentDigest(LAB_FILE);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void digest_changes_when_a_line_value_changes() {
        when(resultApi.findActiveByLabFile(LAB_FILE)).thenReturn(twoResults("1.234"));
        var before = api.contentDigest(LAB_FILE);

        when(resultApi.findActiveByLabFile(LAB_FILE)).thenReturn(twoResults("9.999"));
        var after = api.contentDigest(LAB_FILE);

        assertThat(after).isNotEqualTo(before);
    }

    @Test
    void digest_changes_when_a_result_is_added() {
        when(resultApi.findActiveByLabFile(LAB_FILE)).thenReturn(twoResults("1.234"));
        var before = api.contentDigest(LAB_FILE);

        var threeResults = new java.util.ArrayList<>(twoResults("1.234"));
        threeResults.add(result("ZZZ", "5.555", ResultState.BIOLOGICAL_VALIDATED));
        when(resultApi.findActiveByLabFile(LAB_FILE)).thenReturn(threeResults);
        var after = api.contentDigest(LAB_FILE);

        assertThat(after).isNotEqualTo(before);
    }

    private static List<ResultSummary> twoResults(String firstValue) {
        return List.of(
                result("GLU", firstValue, ResultState.BIOLOGICAL_VALIDATED),
                result("CREA", "55", ResultState.BIOLOGICAL_VALIDATED));
    }

    private static ResultSummary result(String code, String realValue, ResultState state) {
        var line =
                new ResultSummary.LineSummary(
                        new ResultLineId(Ulid.generate()),
                        new AnalysisId(Ulid.generate()),
                        code,
                        1,
                        ResultType.REAL,
                        null,
                        new BigDecimal(realValue),
                        null,
                        "mg/L",
                        2,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null);
        return new ResultSummary(
                new ResultId(Ulid.generate()),
                LAB_FILE,
                new AnalysisRequestId(Ulid.generate()),
                new AnalysisId(Ulid.generate()),
                code,
                state,
                Ulid.generate(),
                T,
                BIOLOGIST,
                T,
                List.of(line),
                List.of(),
                T,
                Ulid.generate());
    }
}
