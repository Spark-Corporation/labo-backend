package org.sparkcorporation.lims.result.infra.pdf;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/** Render model for the CDC §VII per-paillasse worklist (A4 landscape). */
public record PaillasseWorksheetModel(
        String paillasseName,
        String disciplineCode,
        String fromIso,
        String toIso,
        String periodDisplay,
        String printedAtDisplay,
        List<DossierBlock> dossiers,
        LabIdentity labIdentity,
        Locale locale) {

    public PaillasseWorksheetModel {
        dossiers = List.copyOf(dossiers);
    }

    public record DossierBlock(
            String registrationNumber,
            String patientDisplay,
            String patientType,
            @Nullable String dossierComment,
            List<Row> rows) {
        public DossierBlock {
            rows = List.copyOf(rows);
        }
    }

    /** valueDisplay is "" until entered; outOfRange drives bold per CDC §VII. */
    public record Row(
            String dailyId,
            String code,
            String componentCode,
            String valueDisplay,
            String unit,
            String referenceDisplay,
            boolean outOfRange,
            boolean techValidated) {}

    public record LabIdentity(
            String name, String address, String accreditationNumber, String managerName) {}
}
