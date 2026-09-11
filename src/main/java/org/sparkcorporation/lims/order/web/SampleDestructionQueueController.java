package org.sparkcorporation.lims.order.web;

import java.time.Clock;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.infra.SampleRepository;

/**
 * Read-side destruction-overdue queue (CDC Onglet « Conservation »): lists samples past their
 * computed destruction deadline that are not yet recorded as destroyed, ordered by deadline. Reuses
 * the {@code lab-file.read} permission (same audience as the dossier work queues).
 */
@RestController
@RequestMapping("/api/v1/samples/queues")
@Validated
@RequiredArgsConstructor
public class SampleDestructionQueueController {

    private final SampleRepository sampleRepository;
    private final Clock clock;

    @GetMapping("/destruction-due")
    @Operation(
            summary = "List samples overdue for destruction",
            description =
                    "Returns samples whose computed destruction deadline (CDC Onglet « Conservation"
                        + " ») has passed but that are not yet recorded as destroyed, ordered by"
                        + " deadline (most overdue first).")
    @RequiresPermission("lab-file.read")
    public Page<SampleDestructionRow> destructionDue(
            @Parameter(description = "Zero-based page number to return.")
                    @RequestParam(defaultValue = "0")
                    @Min(0)
                    int page,
            @Parameter(description = "Number of items per page (max 200).")
                    @RequestParam(defaultValue = "20")
                    @Min(1)
                    @Max(200)
                    int size) {
        return sampleRepository
                .findDestructionDue(clock.instant(), PageRequest.of(page, size))
                .map(SampleDestructionRow::from);
    }
}
