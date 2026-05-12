package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.result.infra.AnteriorResultRepository;
import org.sparkcorporation.lims.result.infra.AnteriorResultRowProjection;

class AnteriorResultServiceTest {

    private AnteriorResultRepository repository;
    private AnteriorResultService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AnteriorResultRepository.class);
        service = new AnteriorResultService(repository);
    }

    @Test
    void limit_clamps_negative_to_one() {
        var patientId = new PatientId("01HZY000000000000000000000");
        Mockito.when(repository.findHistory(eq("01HZY000000000000000000000"), eq("GLU"), eq(1)))
                .thenReturn(List.of());

        service.anterior(patientId, "GLU", -1);

        Mockito.verify(repository).findHistory("01HZY000000000000000000000", "GLU", 1);
    }

    @Test
    void limit_clamps_large_value_to_twelve() {
        var patientId = new PatientId("01HZY000000000000000000000");
        Mockito.when(repository.findHistory(eq("01HZY000000000000000000000"), eq("GLU"), eq(12)))
                .thenReturn(List.of());

        service.anterior(patientId, "GLU", 99);

        Mockito.verify(repository).findHistory("01HZY000000000000000000000", "GLU", 12);
    }

    @Test
    void default_limit_three_passes_through_unchanged() {
        var patientId = new PatientId("01HZY000000000000000000000");
        Mockito.when(repository.findHistory(eq("01HZY000000000000000000000"), eq("GLU"), eq(3)))
                .thenReturn(List.of());

        service.anterior(patientId, "GLU", 3);

        Mockito.verify(repository).findHistory("01HZY000000000000000000000", "GLU", 3);
    }

    @Test
    void empty_history_returns_empty_list() {
        var patientId = new PatientId("01HZY000000000000000000000");
        Mockito.when(repository.findHistory(Mockito.any(), Mockito.any(), Mockito.anyInt()))
                .thenReturn(List.of());

        var result = service.anterior(patientId, "GLU", 3);

        assertThat(result).isEmpty();
    }

    @Test
    void projection_maps_to_row() {
        var patientId = new PatientId("01HZY000000000000000000000");
        var now = Instant.now();
        AnteriorResultRowProjection projection = stubProjection(now);
        Mockito.when(repository.findHistory(eq("01HZY000000000000000000000"), eq("GLU"), eq(3)))
                .thenReturn(List.of(projection));

        var result = service.anterior(patientId, "GLU", 3);

        assertThat(result).hasSize(1);
        var row = result.get(0);
        assertThat(row.resultId().value()).isEqualTo("01HZX000000000000000000001");
        assertThat(row.lineId().value()).isEqualTo("01HZX000000000000000000002");
        assertThat(row.labFileId().value()).isEqualTo("01HZX000000000000000000003");
        assertThat(row.componentCode()).isEqualTo("GLU");
        assertThat(row.isOutOfRange()).isFalse();
        assertThat(row.biologicalValidatedAt()).isEqualTo(now);
    }

    private AnteriorResultRowProjection stubProjection(Instant validatedAt) {
        return new AnteriorResultRowProjection() {
            @Override
            public String getResultId() {
                return "01HZX000000000000000000001";
            }

            @Override
            public String getLineId() {
                return "01HZX000000000000000000002";
            }

            @Override
            public String getLabFileId() {
                return "01HZX000000000000000000003";
            }

            @Override
            public String getLabFileRegistrationNumber() {
                return "LF-001";
            }

            @Override
            public String getComponentCode() {
                return "GLU";
            }

            @Override
            public String getResultType() {
                return "REAL";
            }

            @Override
            public Long getValueInteger() {
                return null;
            }

            @Override
            public BigDecimal getValueReal() {
                return new BigDecimal("5.5");
            }

            @Override
            public String getValueText() {
                return null;
            }

            @Override
            public String getUnit1Snapshot() {
                return "mmol/L";
            }

            @Override
            public boolean isOutOfRange() {
                return false;
            }

            @Override
            public Instant getBiologicalValidatedAt() {
                return validatedAt;
            }
        };
    }
}
