package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.result.infra.ResultSearchRowProjection;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Unit tests for {@link ResultSearchMapper} covering the null-patient and null-referenceIntervalId
 * branches.
 */
class ResultSearchMapperTest {

    @Test
    void toRow_with_null_patient_leaves_patient_fields_null_and_insured_false() {
        var projection = projection(Ulid.generate(), false, null);

        var row = ResultSearchMapper.toRow(projection, null);

        assertThat(row.patientLabCode()).isNull();
        assertThat(row.patientLastName()).isNull();
        assertThat(row.patientFirstNames()).isNull();
        assertThat(row.patientInsured()).isFalse();
    }

    @Test
    void toRow_with_non_null_patient_maps_patient_fields_and_insured() {
        var patientId = Ulid.generate();
        var projection = projection(patientId, false, null);
        var patient =
                new PatientSummary(
                        new PatientId(patientId),
                        "LAB-260505-001",
                        "DOE",
                        "Jane",
                        Civility.MADAME,
                        Sex.F,
                        null,
                        true,
                        false,
                        false);

        var row = ResultSearchMapper.toRow(projection, patient);

        assertThat(row.patientLabCode()).isEqualTo("LAB-260505-001");
        assertThat(row.patientLastName()).isEqualTo("DOE");
        assertThat(row.patientFirstNames()).isEqualTo("Jane");
        assertThat(row.patientInsured()).isTrue();
    }

    @Test
    void toRow_with_null_reference_interval_id_leaves_field_null() {
        var projection = projection(Ulid.generate(), false, null);

        var row = ResultSearchMapper.toRow(projection, null);

        assertThat(row.referenceIntervalId()).isNull();
    }

    @Test
    void toRow_with_non_null_reference_interval_id_wraps_it() {
        var refId = Ulid.generate();
        var projection = projection(Ulid.generate(), false, refId);

        var row = ResultSearchMapper.toRow(projection, null);

        assertThat(row.referenceIntervalId()).isNotNull();
        assertThat(row.referenceIntervalId().value()).isEqualTo(refId);
    }

    @Test
    void toRow_maps_core_result_fields() {
        var projection = projection(Ulid.generate(), true, null);

        var row = ResultSearchMapper.toRow(projection, null);

        assertThat(row.resultId().value()).isEqualTo(projection.getResultId());
        assertThat(row.isOutOfRange()).isTrue();
        assertThat(row.resultState().name()).isEqualTo("DRAFT");
    }

    // ---- minimal stub projection ----

    private static ResultSearchRowProjection projection(
            String patientOrResultId, boolean outOfRange, String referenceIntervalId) {
        var resultId = Ulid.generate();
        var lineId = Ulid.generate();
        var labFileId = Ulid.generate();
        var now = Instant.now();

        return new ResultSearchRowProjection() {
            @Override
            public String getResultId() {
                return resultId;
            }

            @Override
            public String getLineId() {
                return lineId;
            }

            @Override
            public String getLabFileId() {
                return labFileId;
            }

            @Override
            public String getLabFileRegistrationNumber() {
                return "LF-260505-0001";
            }

            @Override
            public String getPatientId() {
                return patientOrResultId;
            }

            @Override
            public String getAnalysisCode() {
                return "GLU";
            }

            @Override
            public String getComponentCode() {
                return "GLU";
            }

            @Override
            public int getPosition() {
                return 0;
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
                return BigDecimal.valueOf(5.2);
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
                return outOfRange;
            }

            @Override
            public String getReferenceIntervalId() {
                return referenceIntervalId;
            }

            @Override
            public String getResultState() {
                return "DRAFT";
            }

            @Override
            public Instant getBiologicalValidatedAt() {
                return null;
            }

            @Override
            public Instant getCreatedAt() {
                return now;
            }
        };
    }
}
