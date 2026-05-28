package org.sparkcorporation.lims.result.infra.pdf;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.result.api.CompteRenduApi;
import org.sparkcorporation.lims.result.api.CompteRenduPdf;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.api.ResultSummary;

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

    @Override
    public String contentDigest(LabFileId labFileId) {
        var results = resultApi.findActiveByLabFile(labFileId);
        var sb = new StringBuilder();
        results.stream()
                .sorted(java.util.Comparator.comparing(ResultSummary::analysisCodeSnapshot))
                .forEach(
                        r -> {
                            sb.append(r.analysisCodeSnapshot())
                                    .append('|')
                                    .append(r.state())
                                    .append('|')
                                    .append(r.biologicalValidatedBy())
                                    .append('\n');
                            r.lines().stream()
                                    .sorted(
                                            java.util.Comparator.comparingInt(
                                                    ResultSummary.LineSummary::position))
                                    .forEach(
                                            l ->
                                                    sb.append("  ")
                                                            .append(l.componentCodeSnapshot())
                                                            .append('=')
                                                            .append(lineValue(l))
                                                            .append('|')
                                                            .append(l.isOutOfRange())
                                                            .append('\n'));
                        });
        return sha256Hex(sb.toString());
    }

    private static String lineValue(ResultSummary.LineSummary l) {
        if (l.valueInteger() != null) {
            return String.valueOf(l.valueInteger());
        }
        if (l.valueReal() != null) {
            return l.valueReal().toPlainString();
        }
        if (l.valueText() != null) {
            return l.valueText();
        }
        return "";
    }

    private static String sha256Hex(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
