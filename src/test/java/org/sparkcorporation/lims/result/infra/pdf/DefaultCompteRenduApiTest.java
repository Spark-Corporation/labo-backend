package org.sparkcorporation.lims.result.infra.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.api.ResultSummary;

class DefaultCompteRenduApiTest {

    private final ResultApi resultApi = mock(ResultApi.class);
    // assembler / document / pdfService are not exercised by the boolean check, pass mocks
    private final DefaultCompteRenduApi api =
            new DefaultCompteRenduApi(
                    mock(ResultReportModelAssembler.class),
                    mock(ResultReportPdfDocument.class),
                    mock(PdfRenderingService.class),
                    resultApi);

    private final LabFileId labFileId = new LabFileId("01HZXCD0000000000000000002");

    @Test
    void all_bio_validated_true_when_every_result_is_BIOLOGICAL_VALIDATED() {
        var results =
                List.of(
                        summary(ResultState.BIOLOGICAL_VALIDATED),
                        summary(ResultState.BIOLOGICAL_VALIDATED));
        when(resultApi.findActiveByLabFile(labFileId)).thenReturn(results);
        assertThat(api.allResultsBiologicallyValidated(labFileId)).isTrue();
    }

    @Test
    void all_bio_validated_false_when_any_result_not_validated() {
        var results =
                List.of(
                        summary(ResultState.BIOLOGICAL_VALIDATED),
                        summary(ResultState.TECHNICAL_VALIDATED));
        when(resultApi.findActiveByLabFile(labFileId)).thenReturn(results);
        assertThat(api.allResultsBiologicallyValidated(labFileId)).isFalse();
    }

    @Test
    void all_bio_validated_false_when_no_results() {
        when(resultApi.findActiveByLabFile(labFileId)).thenReturn(List.of());
        assertThat(api.allResultsBiologicallyValidated(labFileId)).isFalse();
    }

    private ResultSummary summary(ResultState state) {
        ResultSummary s = mock(ResultSummary.class);
        when(s.state()).thenReturn(state);
        return s;
    }
}
