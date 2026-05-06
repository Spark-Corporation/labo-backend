package org.sparkcorporation.lims.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.domain.Matrix;
import org.sparkcorporation.lims.catalog.domain.Recipient;

class RecipientMapperTest {

    private final RecipientMapper mapper = new RecipientMapper();

    @Test
    void toResponse_maps_all_fields_including_matrix_snapshot() {
        var matrix = Matrix.create("BLOOD", "Whole Blood", null);
        var r =
                Recipient.create(
                        "EDTA_TUBE",
                        "EDTA Tube",
                        "purple cap",
                        "purple",
                        new BigDecimal("4.00"),
                        matrix);
        var resp = mapper.toResponse(r);
        assertThat(resp.code()).isEqualTo("EDTA_TUBE");
        assertThat(resp.name()).isEqualTo("EDTA Tube");
        assertThat(resp.color()).isEqualTo("purple");
        assertThat(resp.volumeMl()).isEqualByComparingTo("4.00");
        assertThat(resp.defaultMatrix()).isNotNull();
        assertThat(resp.defaultMatrix().code()).isEqualTo("BLOOD");
        assertThat(resp.defaultMatrix().id()).isEqualTo(matrix.id().value());
    }

    @Test
    void toResponse_handles_null_default_matrix() {
        var r = Recipient.create("VIAL", "Vial", null, null, null, null);
        var resp = mapper.toResponse(r);
        assertThat(resp.defaultMatrix()).isNull();
        assertThat(resp.color()).isNull();
        assertThat(resp.volumeMl()).isNull();
    }

    @Test
    void toResponse_deletedAt_null_when_active() {
        var r = Recipient.create("TUBE", "Tube", null, null, null, null);
        var resp = mapper.toResponse(r);
        assertThat(resp.deletedAt()).isNull();
    }
}
