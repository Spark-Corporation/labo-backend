package org.sparkcorporation.lims.patient.web;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.patient.api.PatientId;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService service;

    @PostMapping
    @RequiresPermission("patient.create")
    @Operation(
            summary = "Create a patient",
            description =
                    "Creates a new patient record, generating its lab code. Validates that"
                            + " insuranceId/partnerId, when supplied, reference existing records.")
    public ResponseEntity<PatientResponse> create(
            @Valid @RequestBody CreatePatientRequest request,
            @Parameter(
                            description =
                                    "Optional client-supplied key to make retried creation requests"
                                            + " safe to repeat.")
                    @RequestHeader(value = "Idempotency-Key", required = false)
                    String idempotencyKey) {
        var resp = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/patients/" + resp.id())).body(resp);
    }

    @GetMapping("/{id}")
    @RequiresPermission("patient.search")
    @Operation(
            summary = "Get a patient by id",
            description = "Returns a single patient. Returns 404 when the id does not exist.")
    public PatientResponse get(
            @Parameter(description = "ULID of the patient.") @PathVariable String id) {
        return service.get(new PatientId(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("patient.update")
    @Operation(
            summary = "Update a patient",
            description =
                    "Partially updates a patient's demographic, contact and administrative fields."
                            + " Only supplied fields are changed.")
    public PatientResponse update(
            @Parameter(description = "ULID of the patient.") @PathVariable String id,
            @Valid @RequestBody UpdatePatientRequest req) {
        return service.update(new PatientId(id), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("patient.update")
    @Operation(
            summary = "Delete a patient",
            description =
                    "Soft-deletes a patient record; it is excluded from subsequent reads and"
                            + " searches.")
    public void delete(@Parameter(description = "ULID of the patient.") @PathVariable String id) {
        service.delete(new PatientId(id));
    }

    @PostMapping("/{id}/merge")
    @RequiresPermission("patient.merge")
    @Operation(
            summary = "Merge a patient into another",
            description =
                    "Merges the patient identified by the path id (the source, e.g. a duplicate)"
                        + " into the patient identified by targetId (the survivor). The source is"
                        + " marked as merged (mergedIntoId set to the target) and the updated"
                        + " source record is returned; the target record is unchanged by this call."
                        + " Fails with 400 on a self-merge, 409 when the source is already merged,"
                        + " and 404 when either patient does not exist.")
    public PatientResponse merge(
            @Parameter(description = "ULID of the source patient to merge away.") @PathVariable
                    String id,
            @Valid @RequestBody MergePatientRequest req) {
        return service.merge(new PatientId(id), new PatientId(req.targetId()));
    }

    @GetMapping
    @RequiresPermission("patient.search")
    @Operation(
            summary = "Search patients",
            description =
                    "Returns a page of patients matching the given optional filters (name is"
                            + " matched case-insensitively as a substring; the others as exact"
                            + " matches).")
    public PatientSearchResponse search(
            @Parameter(description = "Case-insensitive substring match on the patient's name.")
                    @RequestParam(required = false)
                    String name,
            @Parameter(description = "Exact match on the patient's phone number.")
                    @RequestParam(required = false)
                    String phone,
            @Parameter(description = "Exact match on the patient's identity document number.")
                    @RequestParam(required = false)
                    String idDocumentNumber,
            @Parameter(description = "Exact match on the patient's lab code.")
                    @RequestParam(required = false)
                    String labCode,
            @Parameter(description = "Zero-based page index.")
                    @RequestParam(defaultValue = "0")
                    @Min(0)
                    int page,
            @Parameter(description = "Page size, 1-100.")
                    @RequestParam(defaultValue = "20")
                    @Min(1)
                    @Max(100)
                    int size) {
        Page<PatientResponse> result =
                service.search(name, phone, idDocumentNumber, labCode, page, size);
        return new PatientSearchResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
