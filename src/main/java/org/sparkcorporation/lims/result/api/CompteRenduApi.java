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
}
