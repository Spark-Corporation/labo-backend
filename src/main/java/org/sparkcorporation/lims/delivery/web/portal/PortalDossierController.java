package org.sparkcorporation.lims.delivery.web.portal;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.delivery.infra.PortalAuthFilter;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.result.api.CompteRenduApi;

/**
 * Patient-portal dossier endpoints. Lists a patient's own PUBLISHED dossiers and downloads their
 * compte-rendu PDFs. The authenticated patient id is provided by {@link PortalAuthFilter} as a
 * request attribute; access to anything not owned + PUBLISHED is 404 (indistinguishable from
 * absent, so the portal never confirms the existence of another patient's dossier).
 */
@RestController
@RequestMapping("/api/v1/portal/dossiers")
@RequiredArgsConstructor
public class PortalDossierController {

    private final LabFileApi labFileApi;
    private final CompteRenduApi compteRenduApi;

    @GetMapping
    public List<PortalDossierResponse> list(
            @RequestAttribute(PortalAuthFilter.PATIENT_ID_ATTR) String patientId) {
        return labFileApi.findActiveByPatient(patientId).stream()
                .filter(f -> f.status() == LabFileStatus.PUBLISHED)
                .map(
                        f ->
                                new PortalDossierResponse(
                                        f.id().value(),
                                        f.registrationNumber().value(),
                                        f.createdAt(),
                                        f.analyses().size()))
                .toList();
    }

    @GetMapping("/{labFileId}/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestAttribute(PortalAuthFilter.PATIENT_ID_ATTR) String patientId,
            @PathVariable String labFileId) {
        var summary = labFileApi.findById(new LabFileId(labFileId)).orElse(null);
        if (summary == null
                || !summary.patient().id().equals(patientId)
                || summary.status() != LabFileStatus.PUBLISHED) {
            // 404 — do not distinguish "not yours" from "absent".
            return ResponseEntity.notFound().build();
        }
        var cr = compteRenduApi.renderCompteRendu(new LabFileId(labFileId));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + cr.filename() + "\"")
                .body(cr.bytes());
    }

    public record PortalDossierResponse(
            String id, String registrationNumber, Instant createdAt, int analysesCount) {}
}
