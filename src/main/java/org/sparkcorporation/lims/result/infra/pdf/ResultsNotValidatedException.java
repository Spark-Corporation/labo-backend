package org.sparkcorporation.lims.result.infra.pdf;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Thrown by {@link ResultReportModelAssembler} when a CR PDF is requested for a lab file that still
 * has at least one non-biologically-validated result attached. Mapped to HTTP 409 by {@code
 * ResultProblemDetailAdvice}.
 */
public class ResultsNotValidatedException extends RuntimeException {

    private final String labFileId;

    public ResultsNotValidatedException(LabFileId labFileId) {
        this(labFileId.value());
    }

    public ResultsNotValidatedException(String labFileId) {
        super("results not biologically validated for lab-file " + labFileId);
        this.labFileId = labFileId;
    }

    public String labFileId() {
        return labFileId;
    }
}
