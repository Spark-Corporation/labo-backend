package org.sparkcorporation.lims.pdf.infra;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.pdf.infra.PdfFontRegistry.FontFace;

class PdfFontRegistryTest {

    @Test
    void verifyResources_throwsOnMissingClasspathResource() {
        var bogus = List.of(new FontFace("fonts/DOES_NOT_EXIST.ttf", 400, FontStyle.NORMAL));

        assertThatThrownBy(() -> PdfFontRegistry.verifyResources(bogus))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DOES_NOT_EXIST");
    }

    @Test
    void verifyResources_passesForBundledFace() {
        assertThatCode(
                        () ->
                                PdfFontRegistry.verifyResources(
                                        List.of(
                                                new FontFace(
                                                        "fonts/DejaVuSans.ttf",
                                                        400,
                                                        FontStyle.NORMAL))))
                .doesNotThrowAnyException();
    }
}
