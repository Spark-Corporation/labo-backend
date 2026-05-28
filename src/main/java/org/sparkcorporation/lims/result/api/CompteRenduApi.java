package org.sparkcorporation.lims.result.api;

import org.sparkcorporation.lims.order.api.LabFileId;

public interface CompteRenduApi {

    /**
     * True iff the dossier has at least one result and every active result is BIOLOGICAL_VALIDATED.
     */
    boolean allResultsBiologicallyValidated(LabFileId labFileId);

    /**
     * Renders the compte-rendu PDF.
     *
     * @throws org.sparkcorporation.lims.result.infra.pdf.ResultsNotValidatedException if not all
     *     results are biologically validated
     */
    CompteRenduPdf renderCompteRendu(LabFileId labFileId);

    /**
     * SHA-256 hex digest over the canonical bio-validated result content of the dossier. Stable
     * across calls for unchanged content; changes when any result line value, result state,
     * biological-validator, or the result set itself changes. Used as the cryptographically-signed
     * snapshot of what the biologist signed off on.
     */
    String contentDigest(LabFileId labFileId);
}
