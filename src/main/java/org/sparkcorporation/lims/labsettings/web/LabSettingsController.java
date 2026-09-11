package org.sparkcorporation.lims.labsettings.web;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.labsettings.api.LabSettings;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.labsettings.api.LabSettingsUpdateRequest;

/**
 * Admin endpoint for the lab identity values rendered on documents (CR PDF header / footer; future:
 * receipts, quotes, invoices).
 *
 * <p>Authorization (spec 1.5.e, §9.5 permissions catalog):
 *
 * <ul>
 *   <li>{@code GET} requires {@code lab-settings.read} — seeded to every role that already holds
 *       {@code lab-file.read} (ADMIN, LAB-MANAGER, BIOLOGIST, TECHNICIAN, RECEPTIONIST).
 *   <li>{@code PUT} requires {@code lab-settings.manage} — granted to {@code ROLE_ADMIN} only.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/lab-settings")
@RequiredArgsConstructor
public class LabSettingsController {

    private final LabSettingsApi api;

    @GetMapping
    @RequiresPermission("lab-settings.read")
    @Operation(
            summary = "Get lab identity settings",
            description =
                    "Returns the lab identity values (name, address, accreditation number, manager"
                            + " name) rendered on documents such as the CR PDF header and footer.")
    public LabSettings get() {
        return api.get();
    }

    @PutMapping
    @RequiresPermission("lab-settings.manage")
    @Operation(
            summary = "Update lab identity settings",
            description =
                    "Replaces the lab identity values rendered on documents such as the CR PDF"
                            + " header and footer.")
    public LabSettings update(@Valid @RequestBody LabSettingsUpdateRequest req) {
        return api.update(req);
    }
}
