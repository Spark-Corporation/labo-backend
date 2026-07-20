package org.sparkcorporation.lims.pdf.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

/**
 * Smoke test for chunk 1: proves the four DejaVu .ttf files and the OpenHTMLtoPDF builder class are
 * on the classpath. No Spring context, no rendering — just a sanity gate that the pom changes
 * landed.
 */
class PdfClasspathSmokeTest {

    @Test
    void classpathHasOpenHtmlToPdfBuilderAndFourDejaVuFonts() {
        assertThatCode(() -> Class.forName("com.openhtmltopdf.pdfboxout.PdfRendererBuilder"))
                .as("OpenHTMLtoPDF PdfRendererBuilder must be on the classpath")
                .doesNotThrowAnyException();

        assertThatCode(() -> Class.forName("com.lowagie.text.Document"))
                .as("OpenPDF (com.lowagie root package, iText 5 fork) must be on the classpath")
                .doesNotThrowAnyException();

        for (var face :
                new String[] {
                    "fonts/DejaVuSans.ttf",
                    "fonts/DejaVuSans-Bold.ttf",
                    "fonts/DejaVuSans-Oblique.ttf",
                    "fonts/DejaVuSans-BoldOblique.ttf"
                }) {
            try (var in = getClass().getClassLoader().getResourceAsStream(face)) {
                assertThat(in).as("font %s must exist on the classpath", face).isNotNull();
            } catch (Exception e) {
                throw new AssertionError("font lookup failed for " + face, e);
            }
        }
    }
}
