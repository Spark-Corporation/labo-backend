package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.InsuranceId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.result.infra.DossierRowProjection;
import org.sparkcorporation.lims.result.infra.OverdueDossierRowProjection;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

class DossierMapperTest {

    @Test
    void maps_projection_with_out_of_range_line_to_red_marker() {
        DossierRowProjection projection = stubProjection("RED");
        LabFileSummary labFile = stubLabFile();
        PatientSummary patient = stubPatient(true);

        DossierRow row = DossierMapper.toRow(projection, labFile, patient);

        assertThat(row.marker()).isEqualTo(DossierMarker.RED);
        assertThat(row.labFileId()).isEqualTo(projection.getLabFileId());
        assertThat(row.registrationNumber()).isEqualTo("LF-260511-0001");
        assertThat(row.patientInsured()).isTrue();
        assertThat(row.urgent()).isTrue();
    }

    @Test
    void maps_pen_marker_when_projection_says_pen() {
        DossierRowProjection projection = stubProjection("PEN");
        DossierRow row = DossierMapper.toRow(projection, stubLabFile(), stubPatient(false));
        assertThat(row.marker()).isEqualTo(DossierMarker.PEN);
        assertThat(row.patientInsured()).isFalse();
    }

    @Test
    void maps_green_marker_when_projection_says_green() {
        DossierRowProjection projection = stubProjection("GREEN");
        DossierRow row = DossierMapper.toRow(projection, stubLabFile(), stubPatient(true));
        assertThat(row.marker()).isEqualTo(DossierMarker.GREEN);
    }

    @Test
    void tolerates_null_patient_summary() {
        DossierRowProjection projection = stubProjection("GREEN");
        DossierRow row = DossierMapper.toRow(projection, stubLabFile(), null);
        assertThat(row.patientLabCode()).isNull();
        assertThat(row.patientLastName()).isNull();
        assertThat(row.patientInsured()).isFalse();
    }

    @Test
    void toOverdueRow_composes_inner_dossier_row_and_overdue_info() {
        OverdueDossierRowProjection projection = stubOverdueProjection("PEN");
        LabFileSummary labFile = stubLabFile();
        PatientSummary patient = stubPatient(true);

        OverdueDossierRow row = DossierMapper.toOverdueRow(projection, labFile, patient);

        assertThat(row.dossier().labFileId()).isEqualTo(projection.getLabFileId());
        assertThat(row.dossier().registrationNumber()).isEqualTo("LF-260511-0001");
        assertThat(row.dossier().patientInsured()).isTrue();
        assertThat(row.dossier().marker()).isEqualTo(DossierMarker.PEN);

        assertThat(row.overdue().analysisCode()).isEqualTo("GLY");
        assertThat(row.overdue().analysisName()).isEqualTo("Glycemie a jeun");
        assertThat(row.overdue().deadline()).isEqualTo(Instant.parse("2026-05-13T08:14:00Z"));
        assertThat(row.overdue().hoursPastDeadline()).isEqualTo(47);
        assertThat(row.overdue().overdueAnalysisCount()).isEqualTo(3);
    }

    private OverdueDossierRowProjection stubOverdueProjection(String marker) {
        return new OverdueDossierRowProjection() {
            @Override
            public String getLabFileId() {
                return "01HZX000000000000000000000";
            }

            @Override
            public String getPatientId() {
                return "01HZY000000000000000000000";
            }

            @Override
            public String getMarker() {
                return marker;
            }

            @Override
            public String getAnalysisCode() {
                return "GLY";
            }

            @Override
            public String getAnalysisName() {
                return "Glycemie a jeun";
            }

            @Override
            public Instant getDeadline() {
                return Instant.parse("2026-05-13T08:14:00Z");
            }

            @Override
            public int getHoursPastDeadline() {
                return 47;
            }

            @Override
            public int getOverdueAnalysisCount() {
                return 3;
            }
        };
    }

    private DossierRowProjection stubProjection(String marker) {
        return new DossierRowProjection() {
            @Override
            public String getLabFileId() {
                return "01HZX000000000000000000000";
            }

            @Override
            public String getPatientId() {
                return "01HZY000000000000000000000";
            }

            @Override
            public String getMarker() {
                return marker;
            }
        };
    }

    private LabFileSummary stubLabFile() {
        return new LabFileSummary(
                new LabFileId("01HZX000000000000000000000"),
                new LabFileRegistrationNumber("LF-260511-0001"),
                new LabFileSummary.PatientSnapshot(
                        "01HZY000000000000000000000", "LAB-001", "DOE", "Jane", Sex.F),
                null,
                null,
                true,
                org.sparkcorporation.lims.order.api.LabFileStatus.OPEN,
                java.util.List.of(),
                java.util.List.of(),
                null,
                null,
                new LabFileSummary.BillingBlock(
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, null, null),
                Instant.now(),
                null,
                null,
                null);
    }

    private PatientSummary stubPatient(boolean insured) {
        var insurance =
                insured
                        ? new PatientSummary.InsuranceSnapshot(
                                new InsuranceId(Ulid.generate()), "INS", "Sample Insurance")
                        : null;
        return new PatientSummary(
                new PatientId("01HZY000000000000000000000"),
                "LAB-001",
                "DOE",
                "Jane",
                Civility.MADAME,
                Sex.F,
                null,
                insurance,
                null,
                false,
                false);
    }
}
