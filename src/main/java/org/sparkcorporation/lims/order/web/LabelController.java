package org.sparkcorporation.lims.order.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
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
    @RequiresPermission("lab-file.read")
    public LabelResponse labels(@PathVariable String id) {
        return labelService.labelsFor(new LabFileId(id));
    }
}
