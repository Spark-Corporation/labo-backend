package org.sparkcorporation.lims.stats.web;

import java.time.LocalDate;

import jakarta.validation.constraints.Pattern;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(
            summary = "Get activity recap",
            description =
                    "Returns an activity recap over dossiers registered in the given date range:"
                        + " total dossiers, urgent and biologically-validated counts, and the"
                        + " distinct-patient cohort split (all vs. new, the latter being patients"
                        + " created inside the range). Rejects from > to with 400.")
    public ActivityResponse activity(
            @Parameter(description = "Inclusive start date of the range.")
                    @RequestParam("from")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @Parameter(description = "Inclusive end date of the range.")
                    @RequestParam("to")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @Parameter(description = "Restrict to dossiers billed to this insurance (ULID).")
                    @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "insuranceId must be a valid 26-character ULID")
                    @Nullable String insuranceId,
            @Parameter(description = "Restrict to dossiers linked to this partner lab (ULID).")
                    @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "partnerId must be a valid 26-character ULID")
                    @Nullable String partnerId,
            @Parameter(description = "Restrict to dossiers referred by this prescriber (ULID).")
                    @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "prescriberId must be a valid 26-character ULID")
                    @Nullable String prescriberId,
            @Parameter(description = "Restrict to patients of this sex ('M' or 'F').")
                    @RequestParam(required = false)
                    @Nullable String sex) {
        return ActivityResponse.from(
                readModel.activity(from, to, insuranceId, partnerId, prescriberId, sex));
    }

    @GetMapping("/by-discipline")
    @RequiresPermission("stats.read")
    @Operation(
            summary = "Get result counts by discipline",
            description =
                    "Returns, per discipline, the resulted-line and out-of-range-line counts over"
                        + " dossiers registered in the given date range. Discipline is attributed"
                        + " from each line's component analysis.")
    public ByDisciplineResponse byDiscipline(
            @Parameter(description = "Inclusive start date of the range.")
                    @RequestParam("from")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @Parameter(description = "Inclusive end date of the range.")
                    @RequestParam("to")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @Parameter(description = "Restrict to dossiers billed to this insurance (ULID).")
                    @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "insuranceId must be a valid 26-character ULID")
                    @Nullable String insuranceId,
            @Parameter(description = "Restrict to dossiers linked to this partner lab (ULID).")
                    @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "partnerId must be a valid 26-character ULID")
                    @Nullable String partnerId) {
        return ByDisciplineResponse.from(readModel.byDiscipline(from, to, insuranceId, partnerId));
    }

    @GetMapping("/by-prescriber")
    @RequiresPermission("stats.read")
    @Operation(
            summary = "Get dossier counts by prescriber",
            description =
                    "Returns, per prescriber, the number of dossiers registered in the given date"
                        + " range, ordered by descending count. Dossiers without a prescriber are"
                        + " excluded since they cannot be attributed to one.")
    public ByPrescriberResponse byPrescriber(
            @Parameter(description = "Inclusive start date of the range.")
                    @RequestParam("from")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @Parameter(description = "Inclusive end date of the range.")
                    @RequestParam("to")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @Parameter(
                            description =
                                    "Restrict to dossiers with an analysis request in this"
                                            + " discipline (ULID).")
                    @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "disciplineId must be a valid 26-character ULID")
                    @Nullable String disciplineId) {
        return ByPrescriberResponse.from(readModel.byPrescriber(from, to, disciplineId));
    }

    @GetMapping("/analyses")
    @RequiresPermission("stats.read")
    @Operation(
            summary = "Get result breakdown by analysis",
            description =
                    "Returns, per analysis, the resulted, normal and out-of-range (high/low) result"
                            + " line counts over dossiers registered in the given date range."
                            + " High/low counts only cover out-of-range lines backed by a numeric"
                            + " reference interval; qualitative abnormals contribute to neither.")
    public AnalysesResponse analyses(
            @Parameter(description = "Inclusive start date of the range.")
                    @RequestParam("from")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @Parameter(description = "Inclusive end date of the range.")
                    @RequestParam("to")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @Parameter(description = "Restrict to analyses in this discipline (ULID).")
                    @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "disciplineId must be a valid 26-character ULID")
                    @Nullable String disciplineId) {
        return AnalysesResponse.from(readModel.analyses(from, to, disciplineId));
    }

    @GetMapping("/antibiograms")
    @RequiresPermission("stats.read")
    @Operation(
            summary = "Get antibiogram sensitivity counts",
            description =
                    "Returns, per antibiotic, the sensible/intermediate/resistant reading counts"
                        + " over antibiograms whose owning result was registered in the given date"
                        + " range.")
    public AntibiogramsResponse antibiograms(
            @Parameter(description = "Inclusive start date of the range.")
                    @RequestParam("from")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @Parameter(description = "Inclusive end date of the range.")
                    @RequestParam("to")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @Parameter(
                            description =
                                    "Restrict to results whose analysis belongs to this discipline"
                                            + " (ULID).")
                    @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "disciplineId must be a valid 26-character ULID")
                    @Nullable String disciplineId) {
        return AntibiogramsResponse.from(readModel.antibiograms(from, to, disciplineId));
    }

    @GetMapping("/patients")
    @RequiresPermission("stats.read")
    @Operation(
            summary = "Get patient demographics recap",
            description =
                    "Returns patient demographics over dossiers registered in the given date range:"
                        + " the distinct-patient cohort split (all/new/returning), a breakdown by"
                        + " sex, and a breakdown by age band (derived from each result's"
                        + " patient-category snapshot).")
    public PatientStatsResponse patients(
            @Parameter(description = "Inclusive start date of the range.")
                    @RequestParam("from")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @Parameter(description = "Inclusive end date of the range.")
                    @RequestParam("to")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @Parameter(description = "Restrict to dossiers billed to this insurance (ULID).")
                    @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "insuranceId must be a valid 26-character ULID")
                    @Nullable String insuranceId,
            @Parameter(description = "Restrict to dossiers linked to this partner lab (ULID).")
                    @RequestParam(required = false)
                    @Pattern(
                            regexp = ULID_REGEX,
                            message = "partnerId must be a valid 26-character ULID")
                    @Nullable String partnerId) {
        return PatientStatsResponse.from(readModel.patients(from, to, insuranceId, partnerId));
    }
}
