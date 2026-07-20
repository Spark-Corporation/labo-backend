package org.sparkcorporation.lims.result.web;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.result.infra.AnteriorResultRepository;

/**
 * §IV anterior-results service. Clamps {@code limit} to [1, 12] server-side (defense-in-depth
 * alongside {@code @Min}/@code @Max} Bean Validation on the controller).
 */
@Service
@RequiredArgsConstructor
public class AnteriorResultService {

    /**
     * Maximum number of anterior results the server will return, regardless of the caller's limit.
     */
    static final int MAX_LIMIT = 12;

    private final AnteriorResultRepository repository;

    @Transactional(readOnly = true)
    public List<AnteriorResultRow> anterior(PatientId patientId, String componentCode, int limit) {
        int clamped = Math.max(1, Math.min(MAX_LIMIT, limit));
        return repository.findHistory(patientId.value(), componentCode, clamped).stream()
                .map(AnteriorResultMapper::toRow)
                .toList();
    }
}
