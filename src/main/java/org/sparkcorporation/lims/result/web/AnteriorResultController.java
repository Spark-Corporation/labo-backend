package org.sparkcorporation.lims.result.web;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(
            summary = "List a patient's anterior results for a component",
            description =
                    "Returns the most recent biologically-validated result lines for the given"
                            + " patient and analysis component code, most recent first, bounded by"
                            + " limit. Not paginated: the response is a plain list.")
    public List<AnteriorResultRow> anterior(
            @RequestParam
                    @Parameter(description = "ULID of the patient whose history is queried")
                    @Pattern(
                            regexp = "^[0-9A-HJKMNP-TV-Z]{26}$",
                            message = "patientId must be a valid 26-character ULID")
                    String patientId,
            @RequestParam
                    @Parameter(description = "Analysis component code to filter anterior values on")
                    @NotBlank
                    String componentCode,
            @RequestParam(defaultValue = "3")
                    @Parameter(description = "Maximum number of anterior values to return (1-12)")
                    @Min(1)
                    @Max(12)
                    int limit) {
        return service.anterior(new PatientId(patientId), componentCode, limit);
    }
}
