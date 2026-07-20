package org.sparkcorporation.lims.result.infra.pdf;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.pdf.api.PageSize;
import org.sparkcorporation.lims.pdf.api.PdfDocumentType;

/**
 * {@link PdfDocumentType} for the result compte-rendu (CR) PDF. Lives under {@code
 * result.infra.pdf} per the chunk-7 ArchUnit convention.
 *
 * <p>Spec FFT: {@code OpenHtmlToPdfRenderingServiceIT.render_returnsPdfBytesForResultReport}.
 */
@Component
public class ResultReportPdfDocument implements PdfDocumentType<ResultReportModel> {

    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));

    @Override
    public String id() {
        return "result.compte-rendu";
    }

    @Override
    public String templatePath() {
        return "result/cr";
    }

    @Override
    public Class<ResultReportModel> modelClass() {
        return ResultReportModel.class;
    }

    @Override
    public String defaultFilename(ResultReportModel model) {
        return "CR-%s-%s.pdf"
                .formatted(
                        model.labFile().labFileId(),
                        FILE_DATE.format(model.biologicalValidatedAt()));
    }

    @Override
    public PageSize pageSize() {
        return PageSize.A4_PORTRAIT;
    }
}
