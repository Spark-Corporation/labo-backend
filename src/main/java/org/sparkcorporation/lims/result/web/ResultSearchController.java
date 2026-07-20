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

@Validated
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ResultSearchController {

    private final ResultSearchService service;

    @GetMapping("/search")
    @RequiresPermission("result.enter")
    public ResultSearchResponse search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable LocalDate dateTo,
            @RequestParam(required = false) @Nullable PatientType patientType,
            @RequestParam(required = false) @Nullable SearchResultState resultState,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = "^[A-Z0-9_-]{1,30}$",
                            message = "analysisCode must match ^[A-Z0-9_-]{1,30}$")
                    @Nullable String analysisCode,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        var pageable = PageRequest.of(page, size);
        var filters = new SearchFilters(dateFrom, dateTo, patientType, resultState, analysisCode);
        return ResultSearchResponse.from(service.search(filters, pageable));
    }
}
