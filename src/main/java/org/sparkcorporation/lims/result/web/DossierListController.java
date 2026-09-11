package org.sparkcorporation.lims.result.web;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * §IX state-filter dossier list endpoint. Permission: {@code lab-file.read}. Default sort: {@code
 * lab_files.registration_number DESC}.
 */
@Validated
@RestController
@RequestMapping("/api/v1/dossiers")
@RequiredArgsConstructor
public class DossierListController {

    private final DossierListService service;

    @GetMapping
    @RequiresPermission("lab-file.read")
    @Operation(
            summary = "List dossiers filtered by state and flags",
            description =
                    "Returns a paginated list of dossiers matching the given state / urgent /"
                            + " empty / printed / signed / emailed / patient / date-range filters,"
                            + " sorted by registration number descending by default.")
    public DossierListResponse list(
            @RequestParam(required = false)
                    @Parameter(description = "Dossier lifecycle state to filter on")
                    @Nullable DossierState state,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on the lab file's urgent flag")
                    @Nullable Boolean urgent,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on dossiers with no result lines entered yet")
                    @Nullable Boolean empty,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on whether the dossier has been printed")
                    @Nullable Boolean printed,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on whether the dossier has been signed")
                    @Nullable Boolean signed,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on whether the dossier has been emailed")
                    @Nullable Boolean emailed,
            @RequestParam(required = false)
                    @Parameter(description = "Restrict results to a single patient's dossiers")
                    @Pattern(
                            regexp = "^[0-9A-HJKMNP-TV-Z]{26}$",
                            message = "patientId must be a valid 26-character ULID")
                    @Nullable String patientId,
            @RequestParam(required = false)
                    @Parameter(description = "Only dossiers registered on or after this date")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable LocalDate dateFrom,
            @RequestParam(required = false)
                    @Parameter(description = "Only dossiers registered on or before this date")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable LocalDate dateTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        var pageable = PageRequest.of(page, size);
        return DossierListResponse.from(
                service.list(
                        new DossierFilters(
                                state, urgent, empty, printed, signed, emailed, patientId, dateFrom,
                                dateTo),
                        pageable));
    }
}
