package org.sparkcorporation.lims.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.domain.Matrix;

class MatrixMapperTest {

    private final MatrixMapper mapper = new MatrixMapper();

    @Test
    void toResponse_maps_all_fields() {
        var m = Matrix.create("BLOOD", "Whole Blood", "EDTA tube");
        var resp = mapper.toResponse(m);
        assertThat(resp.id()).isEqualTo(m.id().value());
        assertThat(resp.code()).isEqualTo("BLOOD");
        assertThat(resp.name()).isEqualTo("Whole Blood");
        assertThat(resp.description()).isEqualTo("EDTA tube");
        assertThat(resp.version()).isZero();
    }

    @Test
    void toResponse_handles_null_description() {
        var m = Matrix.create("URINE", "Urine", null);
        var resp = mapper.toResponse(m);
        assertThat(resp.description()).isNull();
    }

    @Test
    void toResponse_deletedAt_null_when_active() {
        var m = Matrix.create("SERUM", "Serum", null);
        var resp = mapper.toResponse(m);
        assertThat(resp.deletedAt()).isNull();
    }
}
