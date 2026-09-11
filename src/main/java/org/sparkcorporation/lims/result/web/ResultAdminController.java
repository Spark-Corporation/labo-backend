package org.sparkcorporation.lims.result.web;

import java.util.List;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.AntibiogramReadingId;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;

/**
 * Admin REST controller for {@link org.sparkcorporation.lims.result.domain.Result} per spec §6.1 +
 * §6.2. 9 endpoints under {@code /api/v1/results/*}, gated by the three result permissions (entry /
 * technical / biological).
 *
 * <p>The PATCH body validation is two-step: (a) {@code @Valid} runs Bean-Validation on the typed-
 * tagged {@link PatchLineRequest.ValueUnion} (when present); (b) the controller method body
 * additionally rejects an empty {@code {}} body (neither {@code value} nor {@code commentText})
 * with a 400 — surfaces via the shared advice's {@link IllegalArgumentException} handler.
 */
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ResultAdminController {

    private final ResultService service;

    @GetMapping("/{id}")
    @RequiresPermission("result.enter")
    @Operation(
            summary = "Get a result by id",
            description =
                    "Returns the full result aggregate (lines + antibiogram readings) for the given"
                            + " result id.")
    public ResultResponse get(@PathVariable String id) {
        return ResultMapper.toResponse(service.get(new ResultId(id)));
    }

    @GetMapping(params = "labFileId")
    @RequiresPermission("result.enter")
    @Operation(
            summary = "List results for a lab file",
            description =
                    "Returns every active result (one per analysis request) attached to the given"
                            + " lab file, each with its lines and antibiogram readings.")
    public List<ResultResponse> findByLabFile(@RequestParam String labFileId) {
        return service.findByLabFile(new LabFileId(labFileId)).stream()
                .map(ResultMapper::toResponse)
                .toList();
    }

    @PatchMapping("/{id}/lines/{lineId}")
    @RequiresPermission("result.enter")
    @Operation(
            summary = "Set a result line's value and/or comment",
            description =
                    "Updates a result line's value and/or free-text comment; at least one of the"
                        + " two must be present or the request is rejected with 400. Setting the"
                        + " value on a FORMULA line is rejected — those are computed server-side."
                        + " Saving all lines flips the result from DRAFT to ENTERED automatically.")
    public ResultResponse patchLine(
            @PathVariable String id,
            @PathVariable String lineId,
            @Valid @RequestBody PatchLineRequest req) {
        if (req.value() == null && req.commentText() == null) {
            throw new IllegalArgumentException(
                    "PATCH body must include 'value' and/or 'commentText'");
        }
        return ResultMapper.toResponse(
                service.patchLine(new ResultId(id), new ResultLineId(lineId), req));
    }

    @DeleteMapping("/{id}/lines/{lineId}/value")
    @RequiresPermission("result.enter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Clear a result line's value",
            description =
                    "Removes the value (and out-of-range flag) previously set on the line, which"
                            + " may regress the parent result's state from ENTERED back to DRAFT.")
    public void clearLineValue(@PathVariable String id, @PathVariable String lineId) {
        service.clearLineValue(new ResultId(id), new ResultLineId(lineId));
    }

    @PostMapping("/{id}/antibiogram")
    @RequiresPermission("result.enter")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add an antibiogram reading",
            description =
                    "Records a measured inhibition diameter for one antibiotic against this result,"
                            + " and computes its sensitivity interpretation (S/I/R) from the"
                            + " antibiotic's catalog breakpoints.")
    public ResultResponse.AntibiogramReadingResponse addAntibiogramReading(
            @PathVariable String id, @Valid @RequestBody AddAntibiogramReadingRequest req) {
        return ResultMapper.toAntibiogramReadingResponse(
                service.addAntibiogramReading(new ResultId(id), req));
    }

    @DeleteMapping("/{id}/antibiogram/{readingId}")
    @RequiresPermission("result.enter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove an antibiogram reading",
            description = "Deletes one previously recorded antibiotic reading from the result.")
    public void removeAntibiogramReading(@PathVariable String id, @PathVariable String readingId) {
        service.removeAntibiogramReading(new ResultId(id), new AntibiogramReadingId(readingId));
    }

    @PostMapping("/{id}:validate-technical")
    @RequiresPermission("result.validate-tech")
    @Operation(
            summary = "Technically validate a result",
            description =
                    "Transitions the result from ENTERED to TECHNICAL_VALIDATED, recording the"
                            + " validating technician and timestamp. Rejected if the result is not"
                            + " currently ENTERED.")
    public ResultResponse validateTechnical(@PathVariable String id) {
        return ResultMapper.toResponse(
                service.validateTechnical(new ResultId(id), currentUserId()));
    }

    @PostMapping("/{id}:validate-biological")
    @RequiresPermission("result.validate-bio")
    @Operation(
            summary = "Biologically validate a result",
            description =
                    "Transitions the result from TECHNICAL_VALIDATED to the terminal"
                        + " BIOLOGICAL_VALIDATED state, recording the validating biologist and"
                        + " timestamp, and triggers downstream billing/delivery/stats processing."
                        + " Rejected if the result is not currently TECHNICAL_VALIDATED.")
    public ResultResponse validateBiological(@PathVariable String id) {
        return ResultMapper.toResponse(
                service.validateBiological(new ResultId(id), currentUserId()));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("result.enter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a result",
            description =
                    "Soft-deletes the result. Only allowed while the result is still in DRAFT"
                            + " state; validated or entered results cannot be deleted.")
    public void delete(@PathVariable String id) {
        service.delete(new ResultId(id));
    }

    private static String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("no authenticated user in SecurityContext");
        }
        return auth.getName();
    }
}
