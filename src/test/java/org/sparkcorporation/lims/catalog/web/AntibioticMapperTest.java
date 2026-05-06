package org.sparkcorporation.lims.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.domain.Antibiotic;

class AntibioticMapperTest {

    private final AntibioticMapper mapper = new AntibioticMapper();

    @Test
    void toResponse_maps_all_fields() {
        var a =
                Antibiotic.create(
                        "AMX",
                        "Amoxicilline",
                        "broad-spectrum",
                        "Beta-lactam",
                        new BigDecimal("20.00"),
                        new BigDecimal("14.00"),
                        new BigDecimal("17.00"));
        var resp = mapper.toResponse(a);
        assertThat(resp.code()).isEqualTo("AMX");
        assertThat(resp.name()).isEqualTo("Amoxicilline");
        assertThat(resp.family()).isEqualTo("Beta-lactam");
        assertThat(resp.referenceDiameter()).isEqualByComparingTo("20.00");
        assertThat(resp.intermediateLower()).isEqualByComparingTo("14.00");
        assertThat(resp.intermediateUpper()).isEqualByComparingTo("17.00");
    }

    @Test
    void toResponse_handles_null_optionals() {
        var a =
                Antibiotic.create(
                        "GEN", "Gentamicine", null, null, new BigDecimal("15.00"), null, null);
        var resp = mapper.toResponse(a);
        assertThat(resp.description()).isNull();
        assertThat(resp.family()).isNull();
        assertThat(resp.intermediateLower()).isNull();
        assertThat(resp.intermediateUpper()).isNull();
    }

    @Test
    void toResponse_deletedAt_null_when_active() {
        var a =
                Antibiotic.create(
                        "TET", "Tetracycline", null, null, new BigDecimal("18.00"), null, null);
        var resp = mapper.toResponse(a);
        assertThat(resp.deletedAt()).isNull();
    }
}
