package org.sparkcorporation.lims.pdf.infra;

import static org.assertj.core.api.Assertions.assertThat;
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

@SpringBootTest
@Import(AsyncPdfWorkerIT.Config.class)
class AsyncPdfWorkerIT extends IntegrationTestBase {

    public record FakeModel(String id, String marker) {}

    public static class FakePdfDocumentType implements PdfDocumentType<FakeModel> {
        @Override
        public String id() {
            return "async.fake";
        }

        @Override
        public String templatePath() {
            return "test/test-doc";
        }

        @Override
        public Class<FakeModel> modelClass() {
            return FakeModel.class;
        }

        @Override
        public String defaultFilename(FakeModel m) {
            return "fake.pdf";
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        FakePdfDocumentType fakePdfDocumentType() {
            return new FakePdfDocumentType();
        }
    }

    @Autowired private PdfRenderingService pdfService;
    @Autowired private PdfRenderJobRepository repo;
    @Autowired private FakePdfDocumentType docType;

    @Test
    void enqueueRendersAsynchronouslyAndPersistsBytes() {
        var jobId =
                pdfService.enqueue(
                        docType, new FakeModel("01HZX0ID00000000000000000A", "async-marker"));

        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(150))
                .untilAsserted(
                        () -> {
                            var row = repo.findById(jobId).orElseThrow();
                            assertThat(row.getStatus()).isEqualTo(PdfRenderJobStatus.DONE);
                        });

        var done = repo.findById(jobId).orElseThrow();
        assertThat(done.getBytes()).isNotNull();
        assertThat(done.getBytes()[0]).isEqualTo((byte) '%');
        assertThat(done.getCompletedAt()).isNotNull();
    }

    @Test
    void findJobReturnsViewForKnownIdAndEmptyForUnknown() {
        var jobId = pdfService.enqueue(docType, new FakeModel("01HZX0ID00000000000000000B", "x"));

        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(150))
                .untilAsserted(
                        () ->
                                assertThat(pdfService.findJob(jobId))
                                        .hasValueSatisfying(
                                                v ->
                                                        assertThat(v.status())
                                                                .isIn(
                                                                        PdfRenderJobStatus.DONE,
                                                                        PdfRenderJobStatus
                                                                                .FAILED)));

        assertThat(pdfService.findJob("does-not-exist")).isEmpty();
    }
}
