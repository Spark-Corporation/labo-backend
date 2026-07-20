package org.sparkcorporation.lims.result.web;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * §IV anterior-results endpoint. Returns the most recent {@code BIOLOGICAL_VALIDATED} result lines
 * for a patient + component code pair, bounded by {@code limit} (default 3, range [1, 12]).
 *
 * <p>NOT paginated — returns {@code List<AnteriorResultRow>} directly.
 */
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
@Validated
public class AnteriorResultController {

    private final AnteriorResultService service;

    @GetMapping("/anterior")
    @RequiresPermission("result.enter")
    public List<AnteriorResultRow> anterior(
            @RequestParam
                    @Pattern(
                            regexp = "^[0-9A-HJKMNP-TV-Z]{26}$",
                            message = "patientId must be a valid 26-character ULID")
                    String patientId,
            @RequestParam @NotBlank String componentCode,
            @RequestParam(defaultValue = "3") @Min(1) @Max(12) int limit) {
        return service.anterior(new PatientId(patientId), componentCode, limit);
    }
}
