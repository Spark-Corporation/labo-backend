package org.sparkcorporation.lims.patient.web;

import java.util.List;

public record PatientSearchResponse(
        List<PatientResponse> content, int page, int size, long totalElements, int totalPages) {}
