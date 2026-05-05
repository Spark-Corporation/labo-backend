package org.sparkcorporation.lims.patient.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.patient.domain.ContactInfo;
import org.sparkcorporation.lims.patient.domain.Guardian;
import org.sparkcorporation.lims.patient.domain.IdDocument;
import org.sparkcorporation.lims.patient.domain.NewPatient;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.patient.domain.PersonName;
import org.sparkcorporation.lims.patient.domain.UpdatePatient;

/** Hand-written mapper between web DTOs and domain types. */
@Component
public class PatientMapper {

    public NewPatient toCreateCommand(CreatePatientRequest req, String passwordHashOrNull) {
        return new NewPatient(
                new PersonName(
                        req.name().lastName(), req.name().firstNames(), req.name().maidenName()),
                req.civility(),
                req.sex(),
                req.birthDate(),
                req.idDocument() == null
                        ? null
                        : new IdDocument(req.idDocument().type(), req.idDocument().number()),
                req.profession(),
                req.address(),
                req.clinicalNotes(),
                new ContactInfo(req.contact().phone(), req.contact().email()),
                req.guardian() == null
                        ? null
                        : new Guardian(
                                req.guardian().name(),
                                req.guardian().phone(),
                                req.guardian().relationship()),
                req.insured(),
                req.portalLogin(),
                passwordHashOrNull);
    }

    public UpdatePatient toUpdateCommand(
            UpdatePatientRequest req, java.util.function.Supplier<String> passwordHasher) {
        var name =
                req.name() == null
                        ? null
                        : new PersonName(
                                req.name().lastName(),
                                req.name().firstNames(),
                                req.name().maidenName());
        var idDoc =
                req.idDocument() == null
                        ? null
                        : new IdDocument(req.idDocument().type(), req.idDocument().number());
        var contact =
                req.contact() == null
                        ? null
                        : new ContactInfo(req.contact().phone(), req.contact().email());
        var guardian =
                req.guardian() == null
                        ? null
                        : new Guardian(
                                req.guardian().name(),
                                req.guardian().phone(),
                                req.guardian().relationship());
        var hash = req.portalPassword() == null ? null : passwordHasher.get();
        return new UpdatePatient(
                name,
                req.civility(),
                req.sex(),
                req.birthDate(),
                idDoc,
                req.profession(),
                req.address(),
                req.clinicalNotes(),
                contact,
                guardian,
                req.insured(),
                req.portalLogin(),
                hash);
    }

    public PatientResponse toResponse(Patient p) {
        var n = p.name();
        var idDoc =
                p.idDocument() == null
                        ? null
                        : new PatientResponse.IdDoc(p.idDocument().type(), p.idDocument().number());
        var contact = new PatientResponse.Contact(p.contact().phone(), p.contact().email());
        var guardian =
                p.guardian() == null
                        ? null
                        : new PatientResponse.Guardian(
                                p.guardian().name(),
                                p.guardian().phone(),
                                p.guardian().relationship());
        return new PatientResponse(
                p.id().value(),
                p.labCode(),
                new PatientResponse.Name(n.lastName(), n.firstNames(), n.maidenName()),
                p.civility(),
                p.sex(),
                p.birthDate(),
                idDoc,
                p.profession(),
                p.address(),
                p.clinicalNotes(),
                contact,
                guardian,
                p.insured(),
                p.portalLogin(),
                p.mergedIntoId(),
                p.isDeleted(),
                p.createdAt(),
                p.updatedAt(),
                p.version());
    }
}
