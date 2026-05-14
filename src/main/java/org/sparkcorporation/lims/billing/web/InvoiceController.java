package org.sparkcorporation.lims.billing.web;

import java.net.URI;
import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.billing.api.InvoiceApi;
import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;
import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.domain.InvoiceAlreadyExistsForLabFileException;
import org.sparkcorporation.lims.billing.domain.InvoiceNotFoundException;
import org.sparkcorporation.lims.billing.infra.BillingFinalizationService;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * REST endpoints for {@code Invoice} (spec §7.2). Three operations:
 *
 * <ul>
 *   <li>{@code GET /api/v1/invoices} — search; permission {@code invoice.read}.
 *   <li>{@code GET /api/v1/invoices/{id}} — detail; permission {@code invoice.read}.
 *   <li>{@code POST /api/v1/invoices} — manual issuance (LAB_MANAGER only); permission {@code
 *       invoice.create}. Returns 409 if an invoice already exists for the lab-file (the listener
 *       fires automatically once bio-validation completes — this is the override for the rare case
 *       a finance user needs to force issuance).
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceApi invoiceApi;
    private final BillingFinalizationService finalizationService;
    private final InvoiceMapper mapper;

    @GetMapping
    @RequiresPermission("invoice.read")
    public InvoiceSearchResponse search(
            @RequestParam(required = false) @Nullable Instant from,
            @RequestParam(required = false) @Nullable Instant to,
            @RequestParam(required = false) @Nullable InvoiceCustomerKind customerKind,
            @RequestParam(required = false) @Nullable String customerId,
            @RequestParam(required = false) @Nullable String labFileId,
            @RequestParam(required = false) @Nullable String patientId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size);
        var labFileIdTyped = labFileId == null ? null : new LabFileId(labFileId);
        var pageResult =
                invoiceApi.search(
                        from, to, customerKind, customerId, labFileIdTyped, patientId, pageable);
        var items = pageResult.map(mapper::toResponse).getContent();
        return new InvoiceSearchResponse(items, pageResult.getTotalElements());
    }

    @GetMapping("/{id}")
    @RequiresPermission("invoice.read")
    public InvoiceResponse get(@PathVariable String id) {
        var iid = InvoiceId.of(id);
        return invoiceApi
                .findById(iid)
                .map(mapper::toResponse)
                .orElseThrow(() -> InvoiceNotFoundException.forId(iid));
    }

    @PostMapping
    @RequiresPermission("invoice.create")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest req) {
        var labFileId = new LabFileId(req.labFileId());

        // Surface 409 explicitly when an invoice was already issued (auto path or earlier manual).
        var existing = invoiceApi.findByLabFileId(labFileId);
        if (existing.isPresent()) {
            throw new InvoiceAlreadyExistsForLabFileException(labFileId, existing.get().id());
        }

        var maybeInvoice = finalizationService.finalize(labFileId);
        if (maybeInvoice.isEmpty()) {
            // 422 — the lab-file is not yet fully bio-validated.
            throw new org.sparkcorporation.lims.billing.domain.InvalidPricingException(
                    "lab file " + labFileId.value() + " is not yet fully biologically validated");
        }
        var summary = invoiceApi.findByLabFileId(labFileId).orElseThrow();
        var body = mapper.toResponse(summary);
        return ResponseEntity.created(URI.create("/api/v1/invoices/" + body.id())).body(body);
    }
}
