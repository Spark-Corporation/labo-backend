package org.sparkcorporation.lims.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.domain.Discipline;

class DisciplineMapperTest {

    private final DisciplineMapper mapper = new DisciplineMapper();

    @Test
    void toResponse_maps_all_fields() {
        var d = Discipline.create("BIO", "Biochimie", "blood chemistry");
        var resp = mapper.toResponse(d);
        assertThat(resp.id()).isEqualTo(d.id().value());
        assertThat(resp.code()).isEqualTo("BIO");
        assertThat(resp.name()).isEqualTo("Biochimie");
        assertThat(resp.description()).isEqualTo("blood chemistry");
        assertThat(resp.version()).isZero();
    }

    @Test
    void toResponse_handles_null_description() {
        var d = Discipline.create("HEM", "Hematology", null);
        var resp = mapper.toResponse(d);
        assertThat(resp.description()).isNull();
    }

    @Test
    void toResponse_deletedAt_null_when_active() {
        var d = Discipline.create("MIC", "Microbiology", null);
        var resp = mapper.toResponse(d);
        assertThat(resp.deletedAt()).isNull();
    }
}
