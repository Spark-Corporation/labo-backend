package org.sparkcorporation.lims.billing.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.billing.api.PaymentId;
import org.sparkcorporation.lims.billing.infra.pdf.PaymentReceiptModelAssembler;
import org.sparkcorporation.lims.billing.infra.pdf.PaymentReceiptPdfDocument;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;

/**
 * Synchronous payment receipt PDF endpoint (spec 1.5.g).
 *
 * <p>{@code GET /api/v1/payments/{id}/receipt.pdf} → A5 {@code application/pdf} bytes inline.
 * Returns 404 when the id is unknown (propagated as {@code PaymentInvalidIdException} → handled by
 * {@link BillingProblemDetailAdvice}).
 *
 * <p>Authorization: {@code invoice.read} — anyone who can view a payment can print its receipt.
 * Reuses the existing permission per spec — no new permission introduced.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentReceiptController {

    private final PaymentReceiptModelAssembler assembler;
    private final PaymentReceiptPdfDocument docType;
    private final PdfRenderingService pdfService;

    @GetMapping("/{id}/receipt.pdf")
    @RequiresPermission("invoice.read")
    public ResponseEntity<byte[]> get(@PathVariable String id) {
        var model = assembler.assemble(new PaymentId(id));
        var rendered = pdfService.render(docType, model);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        "Content-Disposition",
                        "inline; filename=\"" + rendered.suggestedFilename() + "\"")
                .body(rendered.bytes());
    }
}
