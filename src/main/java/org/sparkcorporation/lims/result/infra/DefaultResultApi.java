package org.sparkcorporation.lims.result.infra;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.AntibiogramReadingId;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultSummary;
import org.sparkcorporation.lims.result.api.ResultSummary.AntibiogramReadingSummary;
import org.sparkcorporation.lims.result.api.ResultSummary.LineSummary;
import org.sparkcorporation.lims.result.domain.AntibiogramReading;
import org.sparkcorporation.lims.result.domain.Result;
import org.sparkcorporation.lims.result.domain.ResultLine;
import org.sparkcorporation.lims.result.domain.ResultNotFoundException;

/**
 * Default {@link ResultApi} implementation. Read-only — write paths live in {@code ResultService}
 * (Chunk 5).
 *
 * <p>The two child collections ({@code lines}, {@code antibiogramReadings}) are typed as {@code
 * Set<...>} on {@link Result} (spec §16.6) so a single {@code @EntityGraph} fetch in {@link
 * ResultRepository} hydrates both bags in one query without {@code MultipleBagFetchException} —
 * unlike {@code DefaultLabFileApi}, this implementation does not need an extra round-trip per bag.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultResultApi implements ResultApi {

    private final ResultRepository repo;

    @Override
    public Optional<ResultSummary> findById(ResultId id) {
        return repo.findActiveById(id.value()).map(this::toSummary);
    }

    @Override
    public Optional<ResultSummary> findByAnalysisRequest(AnalysisRequestId id) {
        return repo.findActiveByAnalysisRequestId(id.value()).map(this::toSummary);
    }

    @Override
    public List<ResultSummary> findActiveByLabFile(LabFileId labFileId) {
        return repo.findActiveByLabFileId(labFileId.value()).stream().map(this::toSummary).toList();
    }

    @Override
    public ResultSummary requireById(ResultId id) {
        return repo.findActiveById(id.value())
                .map(this::toSummary)
                .orElseThrow(() -> new ResultNotFoundException(id));
    }

    @Override
    public boolean existsById(ResultId id) {
        return repo.existsActiveById(id.value());
    }

    private ResultSummary toSummary(Result r) {
        var lines = r.getLines().stream().map(DefaultResultApi::toLineSummary).toList();
        var readings =
                r.getAntibiogramReadings().stream()
                        .map(DefaultResultApi::toAntibiogramReadingSummary)
                        .toList();
        return new ResultSummary(
                new ResultId(r.getId()),
                new LabFileId(r.getLabFileId()),
                new AnalysisRequestId(r.getAnalysisRequestId()),
                new AnalysisId(r.getAnalysisId()),
                r.getAnalysisCodeSnapshot(),
                r.getState(),
                r.getTechnicalValidatedBy(),
                r.getTechnicalValidatedAt(),
                r.getBiologicalValidatedBy(),
                r.getBiologicalValidatedAt(),
                lines,
                readings,
                r.getCreatedAt(),
                r.getCreatedBy());
    }

    private static LineSummary toLineSummary(ResultLine l) {
        return new LineSummary(
                new ResultLineId(l.getId()),
                new AnalysisId(l.getComponentAnalysisId()),
                l.getComponentCodeSnapshot(),
                l.getPosition(),
                l.getResultType(),
                l.getValueInteger(),
                l.getValueReal(),
                l.getValueText(),
                l.getUnit1Snapshot(),
                l.getUnit1Decimals(),
                l.getUnit2Snapshot(),
                l.getUnit2Decimals(),
                l.getReferenceIntervalId() == null
                        ? null
                        : new ReferenceIntervalId(l.getReferenceIntervalId()),
                l.isOutOfRange(),
                l.getCommentText());
    }

    private static AntibiogramReadingSummary toAntibiogramReadingSummary(AntibiogramReading ar) {
        return new AntibiogramReadingSummary(
                new AntibiogramReadingId(ar.getId()),
                new AntibioticId(ar.getAntibioticId()),
                ar.getMeasuredDiameter(),
                ar.getInterpretedSensitivity(),
                ar.getPosition());
    }
}
