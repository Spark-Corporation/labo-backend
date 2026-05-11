package org.sparkcorporation.lims.result.api;

import java.time.Instant;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Domain event — fired after technician's {@code :validate-technical} call (ENTERED →
 * TECHNICAL_VALIDATED).
 */
public record TechnicalValidationDone(
        ResultId resultId, LabFileId labFileId, String technicalValidatedBy, Instant occurredAt) {}
