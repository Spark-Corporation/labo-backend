package org.sparkcorporation.lims.export.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.export.api.OfficeDocumentType;
import org.sparkcorporation.lims.export.api.OfficeFormat;

class OfficeDocumentTypeRegistryTest {

    private static OfficeDocumentType<Object> type(String id) {
        return new OfficeDocumentType<>() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public OfficeFormat format() {
                return OfficeFormat.DOCX;
            }

            @Override
            public Class<Object> modelClass() {
                return Object.class;
            }

            @Override
            public String defaultFilename(Object m) {
                return "f.docx";
            }

            @Override
            public byte[] build(Object m) {
                return new byte[0];
            }
        };
    }

    @Test
    void duplicate_id_fails_startup() {
        var registry = new OfficeDocumentTypeRegistry(List.of(type("dup"), type("dup")));
        assertThatThrownBy(registry::validateAfterConstruction)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate OfficeDocumentType id 'dup'");
    }

    @Test
    void byId_returns_registered_type() {
        var registry = new OfficeDocumentTypeRegistry(List.of(type("a"), type("b")));
        registry.validateAfterConstruction();
        assertThat(registry.byId("a").id()).isEqualTo("a");
        assertThat(registry.allIds()).containsExactlyInAnyOrder("a", "b");
    }
}
