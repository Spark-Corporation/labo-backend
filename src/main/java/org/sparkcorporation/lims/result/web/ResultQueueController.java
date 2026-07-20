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
@RequestMapping("/api/v1/results/queues")
@RequiredArgsConstructor
public class ResultQueueController {

    private final ResultQueueService service;

    @GetMapping("/to-validate")
    @RequiresPermission("result.validate-bio")
    public ResultQueueResponse toValidate(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ResultQueueResponse.from(service.findToValidate(PageRequest.of(page, size)));
    }

    @GetMapping("/to-print")
    @RequiresPermission("result.validate-bio")
    public ResultQueueResponse toPrint(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ResultQueueResponse.from(service.findToPrint(PageRequest.of(page, size)));
    }
}
