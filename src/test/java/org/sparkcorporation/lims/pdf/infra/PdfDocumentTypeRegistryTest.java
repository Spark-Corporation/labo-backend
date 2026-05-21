package org.sparkcorporation.lims.pdf.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.pdf.api.PdfDocumentType;

class PdfDocumentTypeRegistryTest {

    private record FakeModel(String x) {}

    private static PdfDocumentType<FakeModel> fakeWithId(String id) {
        return new PdfDocumentType<>() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String templatePath() {
                return "fake/template";
            }

            @Override
            public Class<FakeModel> modelClass() {
                return FakeModel.class;
            }

            @Override
            public String defaultFilename(FakeModel m) {
                return "fake.pdf";
            }
        };
    }

    @Test
    void byIdReturnsRegisteredBean() {
        var a = fakeWithId("alpha");
        var b = fakeWithId("beta");
        var registry = new PdfDocumentTypeRegistry(List.of(a, b));
        registry.validateAfterConstruction();

        assertThat(registry.byId("alpha")).isSameAs(a);
        assertThat(registry.byId("beta")).isSameAs(b);
    }

    @Test
    void byIdUnknownThrows() {
        var registry = new PdfDocumentTypeRegistry(List.of(fakeWithId("alpha")));
        registry.validateAfterConstruction();

        assertThatIllegalStateException()
                .isThrownBy(() -> registry.byId("missing"))
                .withMessageContaining("missing");
    }

    @Test
    void duplicateIdsAreRejectedAtStartup() {
        var dup1 = fakeWithId("same");
        var dup2 = fakeWithId("same");
        var registry = new PdfDocumentTypeRegistry(List.of(dup1, dup2));

        assertThatIllegalStateException()
                .isThrownBy(registry::validateAfterConstruction)
                .withMessageContaining("same")
                .withMessageContaining("duplicate");
    }

    @Test
    void emptyListIsValid() {
        var registry = new PdfDocumentTypeRegistry(List.of());
        registry.validateAfterConstruction();

        assertThat(registry.allIds()).isEmpty();
    }
}
