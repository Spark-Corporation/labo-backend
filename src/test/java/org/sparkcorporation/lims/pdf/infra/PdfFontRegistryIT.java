package org.sparkcorporation.lims.pdf.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.sparkcorporation.lims.IntegrationTestBase;

@SpringBootTest
class PdfFontRegistryIT extends IntegrationTestBase {

    @Autowired private PdfFontRegistry registry;

    @Test
    void startupSmokeRendersMicroGlyphWithDejaVu() {
        // If the bean started, the @PostConstruct + ApplicationReadyEvent smoke
        // already passed. We just confirm the registered family count here.
        assertThat(registry.registeredFamilyName()).isEqualTo("DejaVu Sans");
        assertThat(registry.registeredFaceCount()).isEqualTo(4);
    }
}
