package org.sparkcorporation.lims.order.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * CDC §IV label records — {@code GET /api/v1/lab-files/{id}/labels}. The response carries patient
 * identifying data: never log the body (controller logs nothing; ULIDs only via MDC).
 */
@RestController
@RequestMapping("/api/v1/lab-files")
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;

    @GetMapping("/{id}/labels")
    @Operation(
            summary = "Get printable sample labels for a lab file",
            description =
                    "Returns the thermal-label records (CDC §IV) for every sample on the dossier —"
                        + " patient identity block, analyses, and the number of copies to print per"
                        + " the catalog analysis's label count. The backend never draws the label;"
                        + " the frontend/printer driver renders it and encodes the daily id as the"
                        + " barcode.")
    @RequiresPermission("lab-file.read")
    public LabelResponse labels(@PathVariable String id) {
        return labelService.labelsFor(new LabFileId(id));
    }
}
