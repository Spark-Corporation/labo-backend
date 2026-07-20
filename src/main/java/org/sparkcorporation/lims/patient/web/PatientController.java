package org.sparkcorporation.lims.patient.web;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
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
    public ResponseEntity<PatientResponse> create(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        var resp = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/patients/" + resp.id())).body(resp);
    }

    @GetMapping("/{id}")
    @RequiresPermission("patient.search")
    public PatientResponse get(@PathVariable String id) {
        return service.get(new PatientId(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("patient.update")
    public PatientResponse update(
            @PathVariable String id, @Valid @RequestBody UpdatePatientRequest req) {
        return service.update(new PatientId(id), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("patient.update")
    public void delete(@PathVariable String id) {
        service.delete(new PatientId(id));
    }

    @PostMapping("/{id}/merge")
    @RequiresPermission("patient.merge")
    public PatientResponse merge(
            @PathVariable String id, @Valid @RequestBody MergePatientRequest req) {
        return service.merge(new PatientId(id), new PatientId(req.targetId()));
    }

    @GetMapping
    @RequiresPermission("patient.search")
    public PatientSearchResponse search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String idDocumentNumber,
            @RequestParam(required = false) String labCode,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
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
