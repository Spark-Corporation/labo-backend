package org.sparkcorporation.lims.result.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/lab-files/queues")
@RequiredArgsConstructor
public class LabFileQueueController {

    private final LabFileQueueService service;

    @GetMapping("/urgent")
    @RequiresPermission("lab-file.read")
    public UrgentDossierResponse urgent(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return UrgentDossierResponse.from(service.findUrgent(PageRequest.of(page, size)));
    }

    @GetMapping("/overdue")
    @RequiresPermission("lab-file.read")
    public OverdueDossierResponse overdue(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return OverdueDossierResponse.from(service.findOverdue(PageRequest.of(page, size)));
    }
}
