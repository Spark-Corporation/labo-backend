package org.sparkcorporation.lims.result.web;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.domain.FormulaEvaluator;
import org.sparkcorporation.lims.result.domain.ReferenceIntervalMatcher;
import org.sparkcorporation.lims.result.domain.Result;
import org.sparkcorporation.lims.result.infra.ResultRepository;

/**
 * Recomputes every FORMULA line in a dossier from the dossier's currently-resolved measured values
 * (CDC §Config.8 "Formule", SP11). Runs inside the caller's transaction; sibling {@code Result}
 * aggregates it loads are JPA-managed, so recomputed values flush by dirty-checking — no explicit
 * save.
 *
 * <p>Dependency-ordered (topological sort over formula→referenced-formula-code edges) so a formula
 * may reference another formula. Any formula in a dependency cycle is left blank rather than
 * looping. A formula whose inputs are missing/non-numeric, or that divides by zero, resolves to
 * blank.
 */
@Service
@RequiredArgsConstructor
public class FormulaRecalculationService {

    private final ResultRepository repo;
    private final AnalysisApi analysisApi;

    // Pure/stateless — instantiated directly (initialized final fields are excluded from the
    // Lombok @RequiredArgsConstructor), so it needs no Spring bean definition.
    private final FormulaEvaluator evaluator = new FormulaEvaluator();

    private record FormulaTarget(
            Result result,
            ResultLineId lineId,
            String code,
            String expression,
            AnalysisId analysisId,
            Set<String> deps) {}

    private record Ordered(List<FormulaTarget> resolved, List<FormulaTarget> cyclic) {}

    /** Recompute all formula lines in the dossier; call after any line value changes. */
    public void recompute(String labFileId) {
        List<Result> results = repo.findActiveByLabFileId(labFileId);

        // 1. Base values: measured (non-formula) numeric lines only, by code. Formula codes are
        // added in step 4 as each resolves, so a stale prior-transaction formula value can never
        // seed the map (guards the becomes-cyclic / lost-input case).
        Map<String, BigDecimal> values = new HashMap<>();
        for (Result r : results) {
            values.putAll(r.measuredNumericValues());
        }

        // 2. Collect formula lines with their expression (from the catalog config).
        var targets = new ArrayList<FormulaTarget>();
        var formulaCodes = new HashSet<String>();
        for (Result r : results) {
            // A validated (locked) formula result is immutable — its value is frozen and signed.
            // Don't rewrite it (that would throw ResultLockedException and roll back the edit);
            // just
            // seed its current value so dependent formulas in still-editable results can resolve.
            boolean frozen =
                    r.getState() == ResultState.TECHNICAL_VALIDATED
                            || r.getState() == ResultState.BIOLOGICAL_VALIDATED;
            for (var line : r.formulaLines()) {
                if (frozen) {
                    if (line.getValueReal() != null) {
                        values.put(line.getComponentCodeSnapshot(), line.getValueReal());
                    }
                    continue;
                }
                var analysisId = new AnalysisId(line.getComponentAnalysisId());
                AnalysisSummary a = analysisApi.findById(analysisId).orElse(null);
                String expr = a == null ? null : a.formulaExpression();
                if (expr == null || expr.isBlank()) {
                    r.applyFormula(line.resultLineId(), null, null); // unconfigured → blank
                    continue;
                }
                targets.add(
                        new FormulaTarget(
                                r,
                                line.resultLineId(),
                                line.getComponentCodeSnapshot(),
                                expr,
                                analysisId,
                                evaluator.referencedCodes(expr)));
                formulaCodes.add(line.getComponentCodeSnapshot());
            }
        }

        // 3. Topological order over formula→formula deps; cycles evaluated last as blank.
        Ordered ordered = topoSort(targets, formulaCodes);

        // 4. Evaluate in order, feeding resolved formula values back into the map.
        for (var t : ordered.resolved()) {
            BigDecimal computed = evaluator.evaluate(t.expression(), values).orElse(null);
            ReferenceIntervalSummary ri =
                    computed == null ? null : matchInterval(t.result(), t.analysisId());
            t.result().applyFormula(t.lineId(), computed, ri);
            if (computed != null) {
                values.put(t.code(), computed);
            }
        }
        // 5. Cyclic formulas: explicitly blank (never loop).
        for (var t : ordered.cyclic()) {
            t.result().applyFormula(t.lineId(), null, null);
        }
    }

    /** Kahn's algorithm over edges where a formula depends on another formula code. */
    private static Ordered topoSort(List<FormulaTarget> targets, Set<String> formulaCodes) {
        Map<String, FormulaTarget> byCode = new HashMap<>();
        for (var t : targets) {
            byCode.put(t.code(), t);
        }
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (var t : targets) {
            inDegree.putIfAbsent(t.code(), 0);
            for (String dep : t.deps()) {
                if (formulaCodes.contains(dep) && !dep.equals(t.code())) {
                    inDegree.merge(t.code(), 1, Integer::sum);
                    dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(t.code());
                }
            }
        }
        Deque<String> ready = new ArrayDeque<>();
        for (var e : inDegree.entrySet()) {
            if (e.getValue() == 0) {
                ready.add(e.getKey());
            }
        }
        var resolved = new ArrayList<FormulaTarget>();
        var resolvedCodes = new HashSet<String>();
        while (!ready.isEmpty()) {
            String code = ready.poll();
            resolved.add(byCode.get(code));
            resolvedCodes.add(code);
            for (String dependent : dependents.getOrDefault(code, List.of())) {
                if (inDegree.merge(dependent, -1, Integer::sum) == 0) {
                    ready.add(dependent);
                }
            }
        }
        var cyclic = new ArrayList<FormulaTarget>();
        for (var t : targets) {
            if (!resolvedCodes.contains(t.code())) {
                cyclic.add(t);
            }
        }
        return new Ordered(resolved, cyclic);
    }

    private @Nullable ReferenceIntervalSummary matchInterval(Result r, AnalysisId analysisId) {
        var intervals = analysisApi.findActiveReferenceIntervalsByAnalysis(analysisId);
        return ReferenceIntervalMatcher.match(
                        r.getPatientCategorySnapshot(),
                        r.getPatientSexSnapshot(),
                        r.getPatientAgeInDaysSnapshot(),
                        intervals)
                .orElse(null);
    }
}
