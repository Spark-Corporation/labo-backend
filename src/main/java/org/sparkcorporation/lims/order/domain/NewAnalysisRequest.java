package org.sparkcorporation.lims.order.domain;

/** Command record for adding an {@link AnalysisRequest} to a {@link LabFile}. */
public record NewAnalysisRequest(String analysisId, String analysisCode) {}
