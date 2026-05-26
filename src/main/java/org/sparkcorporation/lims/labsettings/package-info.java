/**
 * `labsettings` module — DB-backed singleton aggregate carrying the lab's display identity (name,
 * address, accreditation number, manager name).
 *
 * <p>Public surface: {@link org.sparkcorporation.lims.labsettings.api}. Consumers (currently {@code
 * result.infra.pdf.ResultReportModelAssembler} for the CR PDF header; future: cashbox receipt,
 * quote, invoice PDFs) call {@code LabSettingsApi.get()}.
 *
 * <p>Spec: docs/superpowers/specs/2026-05-21-lab-settings-design.md (1.5.e).
 */
@NullMarked
@org.springframework.modulith.ApplicationModule(displayName = "Lab Settings")
package org.sparkcorporation.lims.labsettings;

import org.jspecify.annotations.NullMarked;
