package org.sparkcorporation.lims.result.infra.pdf;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.result.api.CompteRenduApi;
import org.sparkcorporation.lims.result.api.CompteRenduPdf;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultState;

@Service
@RequiredArgsConstructor
public class DefaultCompteRenduApi implements CompteRenduApi {

    private final ResultReportModelAssembler assembler;
    private final ResultReportPdfDocument document;
    private final PdfRenderingService pdfService;
    private final ResultApi resultApi;

    @Override
    public boolean allResultsBiologicallyValidated(LabFileId labFileId) {
        var results = resultApi.findActiveByLabFile(labFileId);
        return !results.isEmpty()
                && results.stream().allMatch(r -> r.state() == ResultState.BIOLOGICAL_VALIDATED);
    }

    @Override
    public CompteRenduPdf renderCompteRendu(LabFileId labFileId) {
        var model =
                assembler.assemble(labFileId); // throws ResultsNotValidatedException if not all BV
        var rendered = pdfService.render(document, model);
        return new CompteRenduPdf(rendered.bytes(), rendered.suggestedFilename());
    }
}
