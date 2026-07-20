package org.sparkcorporation.lims.order.infra.pdf;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/** Render model for the CDC §II « feuille de paillasse » (per-dossier bench worksheet). */
public record WorksheetModel(
        Header header,
        PatientBlock patient,
        List<PaillasseBlock> paillasses,
        Footer footer,
        LabIdentity labIdentity,
        Locale locale) {

    public WorksheetModel {
        paillasses = List.copyOf(paillasses);
    }

    public record Header(String registrationNumber, String createdAtDisplay, boolean urgent) {}

    public record PatientBlock(String nom, String prenom, String age, String sexe) {}

    /** One table per paillasse; Résultat column is intentionally blank (handwritten). */
    public record PaillasseBlock(String paillasseName, List<Row> rows) {
        public PaillasseBlock {
            rows = List.copyOf(rows);
        }
    }

    /** dailyId/récipient are "" when the sample is not recorded yet. */
    public record Row(String dailyId, String code, String analysisName, String recipientName) {}

    public record Footer(String printedAtDisplay, @Nullable String preleveurName) {}

    public record LabIdentity(
            String name, String address, String accreditationNumber, String managerName) {}
}
