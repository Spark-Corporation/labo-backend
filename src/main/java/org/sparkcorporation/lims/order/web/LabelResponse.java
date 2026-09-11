package org.sparkcorporation.lims.order.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Response of {@code GET /api/v1/lab-files/{id}/labels} — CDC §IV « Configuration des étiquettes »
 * label records. The frontend / printer driver renders the 4 × 2.5 cm thermal label and encodes
 * {@code dailyId} as the barcode (tech doc §11.3); the backend never draws the label.
 */
@Schema(
        description =
                "Set of thermal-label records to print for the samples on a lab file. The backend"
                    + " never draws the label; the printer driver renders it and encodes dailyId as"
                    + " the barcode.")
public record LabelResponse(
        @Schema(description = "Identifier of the lab file these labels were generated for.")
                String labFileId,
        @Schema(description = "One label record per sample on the dossier.")
                List<LabelRecord> labels) {

    public LabelResponse {
        labels = List.copyOf(labels);
    }

    /** One record per sample; {@code copies} = catalog {@code Analysis.labelCount}. */
    @Schema(description = "One printable label for a single sample.")
    public record LabelRecord(
            @Schema(description = "Identifier of the sample this label is printed for.")
                    String sampleId,
            @Schema(
                            description =
                                    "Daily sequence identifier encoded as the barcode on the"
                                            + " printed label.")
                    String dailyId,
            @Schema(description = "Discipline (lab department) this sample belongs to.")
                    String discipline,
            @Schema(description = "Patient identity block printed on the label.")
                    PatientBlock patient,
            @Schema(description = "Analysis codes covered by this sample, printed on the label.")
                    List<String> analyses,
            @Schema(
                            description =
                                    "Number of physical copies to print for this label, from the"
                                            + " catalog analysis's label count.")
                    int copies) {
        public LabelRecord {
            analyses = List.copyOf(analyses);
        }
    }

    /** CDC label fields: nom, prénom, date de naissance (ISO), âge, sexe. */
    @Schema(
            description =
                    "Patient identity fields printed on the label (CDC §IV): last name, first name,"
                            + " date of birth, age, sex.")
    public record PatientBlock(
            @Schema(description = "Patient's last name (nom).") String nom,
            @Schema(description = "Patient's first name(s) (prénom).") String prenom,
            @Schema(
                            description =
                                    "Patient's date of birth in ISO format (date de naissance), if"
                                            + " known.")
                    @Nullable String dateNaissance,
            @Schema(description = "Patient's age at time of printing (âge).") String age,
            @Schema(description = "Patient's sex (sexe).") String sexe) {}
}
