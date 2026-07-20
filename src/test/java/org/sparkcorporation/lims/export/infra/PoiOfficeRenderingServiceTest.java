package org.sparkcorporation.lims.export.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.export.api.OfficeDocumentType;
import org.sparkcorporation.lims.export.api.OfficeFormat;
import org.sparkcorporation.lims.export.api.OfficeRenderException;

class PoiOfficeRenderingServiceTest {

    private final PoiOfficeRenderingService service =
            new PoiOfficeRenderingService(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    private record FakeModel(String name) {}

    private OfficeDocumentType<FakeModel> docType(byte[] out, RuntimeException boom) {
        return new OfficeDocumentType<>() {
            @Override
            public String id() {
                return "test.fake";
            }

            @Override
            public OfficeFormat format() {
                return OfficeFormat.DOCX;
            }

            @Override
            public Class<FakeModel> modelClass() {
                return FakeModel.class;
            }

            @Override
            public String defaultFilename(FakeModel m) {
                return m.name() + ".docx";
            }

            @Override
            public byte[] build(FakeModel m) {
                if (boom != null) throw boom;
                return out;
            }
        };
    }

    @Test
    void render_delegates_to_build_and_stamps_metadata() {
        var rendered = service.render(docType(new byte[] {7, 8}, null), new FakeModel("DOC"));

        assertThat(rendered.bytes()).containsExactly(7, 8);
        assertThat(rendered.suggestedFilename()).isEqualTo("DOC.docx");
        assertThat(rendered.format()).isEqualTo(OfficeFormat.DOCX);
        assertThat(rendered.generatedAt()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void render_wraps_build_failure_in_OfficeRenderException() {
        assertThatThrownBy(
                        () ->
                                service.render(
                                        docType(null, new IllegalStateException("poi blew up")),
                                        new FakeModel("X")))
                .isInstanceOf(OfficeRenderException.class)
                .hasMessageContaining("test.fake");
    }
}
