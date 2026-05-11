package org.sparkcorporation.lims.result.api;

import java.time.Instant;

import org.sparkcorporation.lims.order.api.LabFileId;

/** Domain event — fired when a save flips the parent {@code Result} from DRAFT to ENTERED. */
public record ResultEntered(ResultId resultId, LabFileId labFileId, Instant occurredAt) {}
