package org.sparkcorporation.lims.patient.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.patient.domain.GuardianRelationship;
import org.sparkcorporation.lims.patient.domain.IdDocumentType;
import org.sparkcorporation.lims.patient.domain.LabCode;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

class PatientMapperTest {

    private final PatientMapper mapper = new PatientMapper();

    @Test
    void toCreateCommand_maps_all_fields() {
        var insuranceId = Ulid.generate();
        var partnerId = Ulid.generate();
        var req =
                new CreatePatientRequest(
                        new CreatePatientRequest.NameRequest("DOE", "Jane", null),
                        Civility.MADAME,
                        Sex.F,
                        null,
                        new CreatePatientRequest.IdDocumentRequest(
                                IdDocumentType.PASSEPORT, "TGO-1"),
                        "infirmière",
                        "Lomé",
                        "notes",
                        new CreatePatientRequest.ContactRequest("+22890000000", "j@e.tg"),
                        new CreatePatientRequest.GuardianRequest(
                                "DOE Sr", null, GuardianRelationship.PARENT),
                        insuranceId,
                        partnerId,
                        "jane.doe",
                        "$argon2id$...");

        var cmd = mapper.toCreateCommand(req, "$argon2id$...");
        assertThat(cmd.name().lastName()).isEqualTo("DOE");
        assertThat(cmd.civility()).isEqualTo(Civility.MADAME);
        assertThat(cmd.idDocument().type()).isEqualTo(IdDocumentType.PASSEPORT);
        assertThat(cmd.guardian().relationship()).isEqualTo(GuardianRelationship.PARENT);
        assertThat(cmd.insuranceId()).isEqualTo(insuranceId);
        assertThat(cmd.partnerId()).isEqualTo(partnerId);
    }

    @Test
    void toResponse_maps_all_fields_from_aggregate() {
        var p =
                Patient.create(
                        new org.sparkcorporation.lims.patient.domain.NewPatient(
                                new org.sparkcorporation.lims.patient.domain.PersonName(
                                        "DOE", "Jane", null),
                                Civility.MADAME,
                                Sex.F,
                                null,
                                null,
                                null,
                                null,
                                null,
                                new org.sparkcorporation.lims.patient.domain.ContactInfo(
                                        null, null),
                                null,
                                null,
                                null,
                                null,
                                null),
                        new LabCode("LAB-260505-001"));
        var resp = mapper.toResponse(p);
        assertThat(resp.id()).isEqualTo(p.id().value());
        assertThat(resp.labCode()).isEqualTo("LAB-260505-001");
        assertThat(resp.name().lastName()).isEqualTo("DOE");
        assertThat(resp.civility()).isEqualTo(Civility.MADAME);
        assertThat(resp.insuranceId()).isNull();
        assertThat(resp.partnerId()).isNull();
    }
}
