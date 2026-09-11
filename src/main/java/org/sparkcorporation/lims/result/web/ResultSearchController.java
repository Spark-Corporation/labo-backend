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

@Validated
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ResultSearchController {

    private final ResultSearchService service;

    @GetMapping("/search")
    @RequiresPermission("result.enter")
    @Operation(
            summary = "Search result lines",
            description =
                    "Returns a paginated, denormalized search over individual result lines,"
                            + " filterable by registration date range, patient type (insured / not"
                            + " insured / society / partner lab), result state band, and analysis"
                            + " code.")
    public ResultSearchResponse search(
            @RequestParam(required = false)
                    @Parameter(description = "Only results registered on or after this date")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable LocalDate dateFrom,
            @RequestParam(required = false)
                    @Parameter(description = "Only results registered on or before this date")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable LocalDate dateTo,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on the patient's billing category")
                    @Nullable PatientType patientType,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on the result's search-state band")
                    @Nullable SearchResultState resultState,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on the analysis code")
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
