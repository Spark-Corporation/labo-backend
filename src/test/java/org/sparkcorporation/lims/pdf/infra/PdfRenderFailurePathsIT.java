package org.sparkcorporation.lims.pdf.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.pdf.api.PdfDocumentType;
import org.sparkcorporation.lims.pdf.api.PdfRenderJobStatus;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.pdf.api.PdfTemplateException;

@SpringBootTest
@Import(PdfRenderFailurePathsIT.Config.class)
class PdfRenderFailurePathsIT extends IntegrationTestBase {

    public record TestModel(String x) {}

    @TestConfiguration
    static class Config {
        @Bean
        PdfDocumentType<TestModel> brokenTemplateDoc() {
            return new PdfDocumentType<>() {
                @Override
                public String id() {
                    return "test.broken-template";
                }

                @Override
                public String templatePath() {
                    return "does/not/exist";
                }

                @Override
                public Class<TestModel> modelClass() {
                    return TestModel.class;
                }

                @Override
                public String defaultFilename(TestModel m) {
                    return "x.pdf";
                }
            };
        }
    }

    @Autowired private PdfRenderingService pdfService;
    @Autowired private PdfRenderJobRepository repo;
    @Autowired private PdfDocumentType<TestModel> brokenTemplateDoc;

    @Test
    void syncRenderOnMissingTemplateThrowsPdfTemplateException() {
        assertThatExceptionOfType(PdfTemplateException.class)
                .isThrownBy(() -> pdfService.render(brokenTemplateDoc, new TestModel("x")))
                .matches(
                        ex ->
                                "template_not_found".equals(ex.reasonTag())
                                        || "template_parse_error".equals(ex.reasonTag()));
    }

    @Test
    void asyncRenderOnMissingTemplateMarksJobFailed() {
        var jobId = pdfService.enqueue(brokenTemplateDoc, new TestModel("x"));

        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(
                        () -> {
                            var row = repo.findById(jobId).orElseThrow();
                            assertThat(row.getStatus()).isEqualTo(PdfRenderJobStatus.FAILED);
                            assertThat(row.getErrorMessage()).startsWith("template_");
                        });
    }
}
