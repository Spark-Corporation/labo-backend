package org.sparkcorporation.lims.result.web;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import dev.sparksam.securedx.rbac.RequiresPermission;
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
    public DossierListResponse list(
            @RequestParam(required = false) @Nullable DossierState state,
            @RequestParam(required = false) @Nullable Boolean urgent,
            @RequestParam(required = false) @Nullable Boolean empty,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = "^[0-9A-HJKMNP-TV-Z]{26}$",
                            message = "patientId must be a valid 26-character ULID")
                    @Nullable String patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable LocalDate dateTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        var pageable = PageRequest.of(page, size);
        return DossierListResponse.from(
                service.list(
                        new DossierFilters(state, urgent, empty, patientId, dateFrom, dateTo),
                        pageable));
    }
}
