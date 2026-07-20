package org.sparkcorporation.lims.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.domain.Technique;

class TechniqueMapperTest {

    private final TechniqueMapper mapper = new TechniqueMapper();

    @Test
    void toResponse_maps_all_fields() {
        var t = Technique.create("ELISA", "Enzyme-linked Immunosorbent", "immuno");
        var resp = mapper.toResponse(t);
        assertThat(resp.id()).isEqualTo(t.id().value());
        assertThat(resp.code()).isEqualTo("ELISA");
        assertThat(resp.name()).isEqualTo("Enzyme-linked Immunosorbent");
        assertThat(resp.description()).isEqualTo("immuno");
        assertThat(resp.version()).isZero();
    }

    @Test
    void toResponse_handles_null_description() {
        var t = Technique.create("PCR", "Polymerase Chain Reaction", null);
        var resp = mapper.toResponse(t);
        assertThat(resp.description()).isNull();
    }

    @Test
    void toResponse_deletedAt_null_when_active() {
        var t = Technique.create("MAN", "Manual", null);
        var resp = mapper.toResponse(t);
        assertThat(resp.deletedAt()).isNull();
    }
}
