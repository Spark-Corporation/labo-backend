package org.sparkcorporation.lims.order.web;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Admin REST controller for managing analyses on an existing OPEN lab-file per spec §7.1.
 *
 * <p>POST {@code /analyses} is a bulk-mutate of an existing parent — returns 200 with the updated
 * {@link LabFileResponse}, NOT 201, because no new top-level resource is created. DELETE is 204.
 */
@RestController
@RequestMapping("/api/v1/lab-files/{labFileId}/analyses")
@RequiredArgsConstructor
public class AnalysisRequestAdminController {

    private final AnalysisRequestService service;

    @PostMapping
    @RequiresPermission("lab-file.create")
    public LabFileResponse addAnalyses(
            @PathVariable String labFileId, @Valid @RequestBody AddAnalysesRequest req) {
        return service.addAnalyses(new LabFileId(labFileId), req.analyses());
    }

    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("lab-file.create")
    public void remove(@PathVariable String labFileId, @PathVariable String requestId) {
        service.removeAnalysis(new LabFileId(labFileId), new AnalysisRequestId(requestId));
    }
}
