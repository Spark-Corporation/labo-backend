package org.sparkcorporation.lims.pdf.infra;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.PostConstruct;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PdfFontRegistry {

    private static final String FAMILY = "DejaVu Sans";

    /** Byte length of the {@code %PDF} magic header at the start of every valid PDF stream. */
    private static final int PDF_MAGIC_LENGTH = 4;

    private record FontFace(String classpath, int weight, FontStyle style) {}

    private static final List<FontFace> FACES =
            List.of(
                    new FontFace("fonts/DejaVuSans.ttf", 400, FontStyle.NORMAL),
                    new FontFace("fonts/DejaVuSans-Bold.ttf", 700, FontStyle.NORMAL),
                    new FontFace("fonts/DejaVuSans-Oblique.ttf", 400, FontStyle.ITALIC),
                    new FontFace("fonts/DejaVuSans-BoldOblique.ttf", 700, FontStyle.ITALIC));

    @PostConstruct
    public void verifyResources() {
        for (var face : FACES) {
            var res = new ClassPathResource(face.classpath());
            if (!res.exists()) {
                throw new IllegalStateException("missing bundled font: " + face.classpath());
            }
        }
    }

    public void applyTo(PdfRendererBuilder builder) {
        for (var face : FACES) {
            builder.useFont(
                    () -> openFont(face.classpath()), FAMILY, face.weight(), face.style(), true);
        }
    }

    private static InputStream openFont(String classpath) {
        try {
            return Objects.requireNonNull(
                    PdfFontRegistry.class.getClassLoader().getResourceAsStream(classpath),
                    "missing font resource: " + classpath);
        } catch (Exception e) {
            throw new IllegalStateException("cannot open font resource " + classpath, e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void smokeRenderAtStartup() {
        var html =
                """
                <html><body style="font-family: 'DejaVu Sans';">µ°±€ — DejaVu Sans smoke</body></html>
                """;
        try (var out = new ByteArrayOutputStream()) {
            var builder =
                    new PdfRendererBuilder()
                            .useFastMode()
                            .withHtmlContent(html, "classpath:/")
                            .toStream(out);
            applyTo(builder);
            builder.run();
            var bytes = out.toByteArray();
            if (bytes.length < PDF_MAGIC_LENGTH
                    || bytes[0] != '%'
                    || bytes[1] != 'P'
                    || bytes[2] != 'D'
                    || bytes[3] != 'F') {
                throw new IllegalStateException("smoke render did not produce a %PDF stream");
            }
            log.info("pdf font registry smoke OK ({} bytes)", bytes.length);
        } catch (Exception e) {
            throw new IllegalStateException("pdf font registry smoke FAILED", e);
        }
    }

    public String registeredFamilyName() {
        return FAMILY;
    }

    public int registeredFaceCount() {
        return FACES.size();
    }
}
