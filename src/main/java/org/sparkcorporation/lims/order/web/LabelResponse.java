package org.sparkcorporation.lims.order.web;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Response of {@code GET /api/v1/lab-files/{id}/labels} — CDC §IV « Configuration des étiquettes »
 * label records. The frontend / printer driver renders the 4 × 2.5 cm thermal label and encodes
 * {@code dailyId} as the barcode (tech doc §11.3); the backend never draws the label.
 */
public record LabelResponse(String labFileId, List<LabelRecord> labels) {

    public LabelResponse {
        labels = List.copyOf(labels);
    }

    /** One record per sample; {@code copies} = catalog {@code Analysis.labelCount}. */
    public record LabelRecord(
            String sampleId,
            String dailyId,
            String discipline,
            PatientBlock patient,
            List<String> analyses,
            int copies) {
        public LabelRecord {
            analyses = List.copyOf(analyses);
        }
    }

    /** CDC label fields: nom, prénom, date de naissance (ISO), âge, sexe. */
    public record PatientBlock(
            String nom, String prenom, @Nullable String dateNaissance, String age, String sexe) {}
}
