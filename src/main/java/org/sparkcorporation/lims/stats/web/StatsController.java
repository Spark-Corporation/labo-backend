package org.sparkcorporation.lims.stats.web;

import java.time.LocalDate;

import jakarta.validation.constraints.Pattern;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.stats.infra.StatsReadModel;

/**
 * Read-only REST endpoints for the CDC "Onglet Statistiques" (spec §5). All endpoints require
 * {@code stats.read} (already seeded/granted to ADMIN, LAB_MANAGER, BIOLOGIST, ACCOUNTANT) and
 * accept a required {@code from}/{@code to} date range; {@code from > to} is rejected with 400 via
 * the read model's {@code IllegalArgumentException} (mapped by the global {@code
 * ProblemDetailAdvice}).
 */
@Validated
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private static final String ULID_REGEX = "^[0-9A-HJKMNP-TV-Z]{26}$";

    private final StatsReadModel readModel;

    @GetMapping("/activity")
    @RequiresPermission("stats.read")
    public ActivityResponse activity(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "insuranceId must be a valid 26-character ULID")
                    @Nullable String insuranceId,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "partnerId must be a valid 26-character ULID")
                    @Nullable String partnerId,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "prescriberId must be a valid 26-character ULID")
                    @Nullable String prescriberId,
            @RequestParam(required = false) @Nullable String sex) {
        return ActivityResponse.from(
                readModel.activity(from, to, insuranceId, partnerId, prescriberId, sex));
    }

    @GetMapping("/by-discipline")
    @RequiresPermission("stats.read")
    public ByDisciplineResponse byDiscipline(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "insuranceId must be a valid 26-character ULID")
                    @Nullable String insuranceId,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "partnerId must be a valid 26-character ULID")
                    @Nullable String partnerId) {
        return ByDisciplineResponse.from(readModel.byDiscipline(from, to, insuranceId, partnerId));
    }

    @GetMapping("/by-prescriber")
    @RequiresPermission("stats.read")
    public ByPrescriberResponse byPrescriber(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "disciplineId must be a valid 26-character ULID")
                    @Nullable String disciplineId) {
        return ByPrescriberResponse.from(readModel.byPrescriber(from, to, disciplineId));
    }

    @GetMapping("/analyses")
    @RequiresPermission("stats.read")
    public AnalysesResponse analyses(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "disciplineId must be a valid 26-character ULID")
                    @Nullable String disciplineId) {
        return AnalysesResponse.from(readModel.analyses(from, to, disciplineId));
    }

    @GetMapping("/antibiograms")
    @RequiresPermission("stats.read")
    public AntibiogramsResponse antibiograms(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "disciplineId must be a valid 26-character ULID")
                    @Nullable String disciplineId) {
        return AntibiogramsResponse.from(readModel.antibiograms(from, to, disciplineId));
    }

    @GetMapping("/patients")
    @RequiresPermission("stats.read")
    public PatientStatsResponse patients(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "insuranceId must be a valid 26-character ULID")
                    @Nullable String insuranceId,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "partnerId must be a valid 26-character ULID")
                    @Nullable String partnerId) {
        return PatientStatsResponse.from(readModel.patients(from, to, insuranceId, partnerId));
    }
}
