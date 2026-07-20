package org.sparkcorporation.lims.catalog.infra;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.AnalyticalDelaySummary;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.ComponentLink;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.DisciplineSnapshot;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.MatrixSnapshot;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.RecipientSnapshot;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.TechniqueLink;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.UnitsSummary;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;
import org.sparkcorporation.lims.catalog.api.TechniqueId;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.AnalysisNotFoundException;
import org.sparkcorporation.lims.catalog.domain.ReferenceInterval;

/**
 * Default {@link AnalysisApi} implementation. Read-only — write paths live in {@code
 * AnalysisService} (Chunk 5).
 *
 * <p><b>Snapshot resolution.</b> {@link AnalysisRepository#findActiveById} fetches single-valued
 * relationships ({@code discipline}, {@code matrix}, {@code recipient}) eagerly via JOIN FETCH.
 * Components and techniques are resolved with two extra batched queries ({@link
 * AnalysisRepository#findChildrenForParent} and {@link AnalysisRepository#findTechniqueLinks}) to
 * avoid Hibernate's {@code MultipleBagFetchException}. Total round-trips per {@link #findById}
 * call: 3 (no N+1).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultAnalysisApi implements AnalysisApi {

    private final AnalysisRepository repo;
    private final ReferenceIntervalRepository referenceIntervalRepo;

    @Override
    public Optional<AnalysisSummary> findById(AnalysisId id) {
        return repo.findActiveById(id.value()).map(this::toSummary);
    }

    @Override
    public Optional<AnalysisSummary> findByCode(AnalysisCode code) {
        return repo.findByCode(code.value()).filter(Analysis::isActive).map(this::toSummary);
    }

    @Override
    public AnalysisSummary requireById(AnalysisId id) {
        return repo.findActiveById(id.value())
                .map(this::toSummary)
                .orElseThrow(() -> new AnalysisNotFoundException(id));
    }

    @Override
    public boolean existsById(AnalysisId id) {
        return repo.findActiveById(id.value()).isPresent();
    }

    @Override
    public List<AnalysisSummary> findAllActive() {
        return repo.findAllActive().stream().map(this::toSummary).toList();
    }

    @Override
    public List<AnalysisSummary> findActiveByDiscipline(DisciplineId disciplineId) {
        return repo.findActiveByDiscipline(disciplineId.value()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public Map<AnalysisId, AnalysisSummary> findAllByIds(Collection<AnalysisId> ids) {
        Objects.requireNonNull(ids, "ids");
        if (ids.isEmpty()) {
            return Map.of();
        }
        var rawIds = ids.stream().map(AnalysisId::value).toList();
        var found = repo.findAllActiveByIdIn(rawIds);
        var byId = new LinkedHashMap<AnalysisId, AnalysisSummary>(found.size());
        for (var a : found) {
            var summary = toSummary(a);
            byId.put(summary.id(), summary);
        }
        return byId;
    }

    @Override
    public List<ReferenceIntervalSummary> findActiveReferenceIntervalsByAnalysis(
            AnalysisId analysisId) {
        Objects.requireNonNull(analysisId, "analysisId");
        return referenceIntervalRepo.findActiveByAnalysisId(analysisId.value()).stream()
                .map(DefaultAnalysisApi::toReferenceIntervalSummary)
                .toList();
    }

    private static ReferenceIntervalSummary toReferenceIntervalSummary(ReferenceInterval ri) {
        return new ReferenceIntervalSummary(
                ri.id(),
                ri.analysisId(),
                ri.category(),
                ri.sex(),
                ri.ageMin(),
                ri.ageMax(),
                ri.ageUnit(),
                ri.mode(),
                ri.unit1Min(),
                ri.unit1Max(),
                ri.unit2Min(),
                ri.unit2Max(),
                ri.commentText());
    }

    private AnalysisSummary toSummary(Analysis a) {
        var discipline = a.discipline();
        if (discipline == null) {
            // FK is NOT NULL — only possible if the Liquibase invariant is violated in tests.
            throw new IllegalStateException(
                    "Analysis " + a.rawId() + " has no discipline (FK violation)");
        }
        var disciplineSnapshot =
                new DisciplineSnapshot(
                        discipline.id(),
                        discipline.code(),
                        discipline.name(),
                        discipline.disciplinePrefix());

        var matrix = a.matrix();
        MatrixSnapshot matrixSnapshot =
                matrix == null
                        ? null
                        : new MatrixSnapshot(matrix.id(), matrix.code(), matrix.name());

        var recipient = a.recipient();
        RecipientSnapshot recipientSnapshot =
                recipient == null
                        ? null
                        : new RecipientSnapshot(recipient.id(), recipient.code(), recipient.name());

        var units = a.units();
        var unitsSummary =
                units == null
                        ? new UnitsSummary(null, null, null, null, null)
                        : new UnitsSummary(
                                units.unit1(),
                                units.unit1Decimals(),
                                units.unit2(),
                                units.unit2Decimals(),
                                units.conversionFactor());

        var delay = a.delay();
        var delaySummary =
                delay == null
                        ? new AnalyticalDelaySummary(null, null, null)
                        : new AnalyticalDelaySummary(
                                delay.days(), delay.hours(), delay.anteriorityCount());

        var components =
                repo.findChildrenForParent(a.rawId()).stream()
                        .map(DefaultAnalysisApi::toComponentLink)
                        .toList();

        var techniques =
                repo.findTechniqueLinks(a.rawId()).stream()
                        .map(DefaultAnalysisApi::toTechniqueLink)
                        .toList();

        return new AnalysisSummary(
                a.id(),
                a.code(),
                a.name(),
                a.kind(),
                a.indiciaryValueB(),
                a.labelCount(),
                disciplineSnapshot,
                matrixSnapshot,
                recipientSnapshot,
                a.resultType(),
                a.interpretationText(),
                a.formulaExpression(),
                a.defaultPortoir(),
                a.defaultConservationDays(),
                unitsSummary,
                delaySummary,
                components,
                techniques);
    }

    private static ComponentLink toComponentLink(Object[] row) {
        // (childId, childCode, childName, position) — see findChildrenForParent.
        return new ComponentLink(
                AnalysisId.of((String) row[0]),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).intValue());
    }

    private static TechniqueLink toTechniqueLink(Object[] row) {
        // (techniqueId, code, name, isDefault) — see findTechniqueLinks.
        return new TechniqueLink(
                new TechniqueId((String) row[0]),
                (String) row[1],
                (String) row[2],
                (Boolean) row[3]);
    }
}
