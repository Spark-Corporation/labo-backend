package org.sparkcorporation.lims.pdf.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PdfRenderExceptionTest {

    @Test
    void reasonTagAndDocumentTypeIdAreExposed() {
        var cause = new RuntimeException("boom");
        var ex = new PdfTemplateException("result.compte-rendu", "template_not_found", cause);

        assertThat(ex.documentTypeId()).isEqualTo("result.compte-rendu");
        assertThat(ex.reasonTag()).isEqualTo("template_not_found");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).contains("result.compte-rendu").contains("template_not_found");
    }

    @Test
    void allFourSubclassesShareTheBase() {
        assertThat(new PdfTemplateException("x", "r", null)).isInstanceOf(PdfRenderException.class);
        assertThat(new PdfModelException("x", "r", null)).isInstanceOf(PdfRenderException.class);
        assertThat(new PdfEngineException("x", "r", null)).isInstanceOf(PdfRenderException.class);
        assertThat(new PdfJobNotFoundException("job-id")).isInstanceOf(PdfRenderException.class);
    }

    @Test
    void pdfJobNotFoundExceptionCarriesTheJobId() {
        var ex = new PdfJobNotFoundException("01HZX0JOB000000000000000000");
        assertThat(ex.jobId()).isEqualTo("01HZX0JOB000000000000000000");
        assertThat(ex.reasonTag()).isEqualTo("job_not_found");
    }
}
