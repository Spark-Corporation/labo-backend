package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.domain.AnalysisNotFoundException;
import org.sparkcorporation.lims.catalog.infra.AnalysisRepository;
import org.sparkcorporation.lims.catalog.web.AnalysisHistoryEntry.Scope;

/**
 * Builds the configuration-modification history of an analysis (SP12, CDC Configurations §8/§9) by
 * reading the Envers {@code *_aud} tables and {@code revinfo} via native SQL. Native access keeps
 * the catalog module free of any {@code iam} type dependency (the {@code revinfo.user_id} column is
 * owned by {@code iam.infra.LimsRevisionEntity}, which catalog cannot import); table access is
 * invisible to the Spring Modulith package-dependency check.
 *
 * <p>The Analysis aggregate's own fields are diffed adjacent-revision to produce a changed-field
 * list; reference intervals, grouped components and technique links are audited as independent
 * entities and surfaced as their own per-revision timelines. All sources merge into one
 * most-recent-first timeline, paginated in memory (config-edit volume per analysis is low).
 */
@Service
@RequiredArgsConstructor
public class AnalysisHistoryService {

    private static final int REVTYPE_ADD = 0;
    private static final int REVTYPE_DEL = 2;

    private final EntityManager em;
    private final AnalysisRepository analysisRepo;

    private record Raw(
            long revision, Scope scope, List<String> changedFields, @Nullable Object snapshot) {}

    private record RevMeta(@Nullable String userId, Instant timestamp) {}

    @Transactional(readOnly = true)
    public AnalysisHistoryResponse history(AnalysisId id, int page, int size) {
        String aid = id.value();

        List<Raw> raws = new ArrayList<>();
        raws.addAll(analysisScalarTimeline(aid));
        raws.addAll(
                childTimeline(
                        aid,
                        "reference_intervals_aud",
                        "analysis_id",
                        "id",
                        List.of(
                                "category",
                                "sex",
                                "age_min",
                                "age_max",
                                "age_unit",
                                "mode",
                                "unit_1_min",
                                "unit_1_max",
                                "unit_2_min",
                                "unit_2_max",
                                "comment_text"),
                        Scope.REFERENCE_INTERVALS,
                        "reference intervals"));
        raws.addAll(
                childTimeline(
                        aid,
                        "analysis_components_aud",
                        "parent_id",
                        "child_id",
                        List.of("position"),
                        Scope.COMPONENTS,
                        "components"));
        raws.addAll(
                childTimeline(
                        aid,
                        "analysis_techniques_aud",
                        "analysis_id",
                        "technique_id",
                        List.of("is_default"),
                        Scope.TECHNIQUES,
                        "techniques"));

        if (raws.isEmpty() && analysisRepo.findById(aid).isEmpty()) {
            throw new AnalysisNotFoundException(id);
        }

        Map<Long, RevMeta> meta =
                revMeta(raws.stream().map(Raw::revision).collect(Collectors.toSet()));

        List<AnalysisHistoryEntry> all =
                raws.stream()
                        .map(
                                r -> {
                                    RevMeta m =
                                            meta.getOrDefault(
                                                    r.revision(), new RevMeta(null, Instant.EPOCH));
                                    return new AnalysisHistoryEntry(
                                            r.revision(),
                                            m.timestamp(),
                                            m.userId(),
                                            r.scope(),
                                            r.changedFields(),
                                            r.snapshot());
                                })
                        .sorted(
                                Comparator.comparingLong(AnalysisHistoryEntry::revision)
                                        .reversed()
                                        .thenComparing(e -> e.scope().name()))
                        .toList();

        return paginate(all, page, size);
    }

    /**
     * In-memory pagination, clamping pathological page/size so subList never throws (→ HTTP 500).
     */
    private static AnalysisHistoryResponse paginate(
            List<AnalysisHistoryEntry> all, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        int total = all.size();
        int from = (int) Math.min((long) p * s, total);
        int to = (int) Math.min((long) from + s, total);
        int totalPages = (int) Math.ceil((double) total / s);
        return new AnalysisHistoryResponse(all.subList(from, to), p, s, total, totalPages);
    }

    private List<Raw> analysisScalarTimeline(String aid) {
        @SuppressWarnings("unchecked")
        List<Tuple> rows =
                em.createNativeQuery(
                                """
                                SELECT rev, revtype, name, indiciary_value_b, label_count,
                                       result_type, kind, interpretation_text, formula_expression,
                                       default_portoir, default_conservation_days, matrix_id,
                                       recipient_id, unit_1, unit_1_decimals, unit_2,
                                       unit_2_decimals, unit_1_to_2_factor, delay_days, delay_hours,
                                       anteriority_count
                                FROM analyses_aud
                                WHERE id = :aid
                                ORDER BY rev ASC
                                """,
                                Tuple.class)
                        .setParameter("aid", aid)
                        .getResultList();

        List<Raw> result = new ArrayList<>();
        AnalysisSnapshot prev = null;
        for (Tuple t : rows) {
            long rev = num(t.get("rev")).longValue();
            int revtype = num(t.get("revtype")).intValue();
            Object labelCount = t.get("label_count");
            AnalysisSnapshot snap =
                    new AnalysisSnapshot(
                            str(t.get("name")),
                            bd(t.get("indiciary_value_b")),
                            labelCount == null ? 0 : num(labelCount).intValue(),
                            str(t.get("result_type")),
                            str(t.get("kind")),
                            str(t.get("interpretation_text")),
                            str(t.get("formula_expression")),
                            str(t.get("default_portoir")),
                            integer(t.get("default_conservation_days")),
                            str(t.get("matrix_id")),
                            str(t.get("recipient_id")),
                            str(t.get("unit_1")),
                            integer(t.get("unit_1_decimals")),
                            str(t.get("unit_2")),
                            integer(t.get("unit_2_decimals")),
                            bd(t.get("unit_1_to_2_factor")),
                            integer(t.get("delay_days")),
                            integer(t.get("delay_hours")),
                            integer(t.get("anteriority_count")));
            List<String> changed;
            if (revtype == REVTYPE_ADD) {
                changed = List.of("created");
            } else if (revtype == REVTYPE_DEL) {
                changed = List.of("deleted");
            } else {
                changed = prev == null ? List.of() : AnalysisSnapshotDiff.diff(prev, snap);
            }
            result.add(new Raw(rev, Scope.ANALYSIS, changed, snap));
            prev = snap;
        }
        return result;
    }

    private List<Raw> childTimeline(
            String aid,
            String table,
            String fkColumn,
            String idColumn,
            List<String> dataColumns,
            Scope scope,
            String label) {
        String cols = String.join(", ", dataColumns);
        String sql =
                "SELECT rev, revtype, "
                        + idColumn
                        + (cols.isEmpty() ? "" : ", " + cols)
                        + " FROM "
                        + table
                        + " WHERE "
                        + fkColumn
                        + " = :aid ORDER BY rev ASC";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).setParameter("aid", aid).getResultList();

        // Group the per-row deltas by revision, preserving revision order.
        Map<Long, List<ChildChange>> byRev = new LinkedHashMap<>();
        Map<Long, int[]> counts = new LinkedHashMap<>(); // [add, mod, del]
        for (Object[] r : rows) {
            long rev = num(r[0]).longValue();
            int revtype = num(r[1]).intValue();
            String childId = str(r[2]);
            Map<String, Object> fields = new LinkedHashMap<>();
            for (int i = 0; i < dataColumns.size(); i++) {
                fields.put(dataColumns.get(i), r[3 + i]);
            }
            byRev.computeIfAbsent(rev, k -> new ArrayList<>())
                    .add(new ChildChange(childId, revLabel(revtype), fields));
            int[] c = counts.computeIfAbsent(rev, k -> new int[3]);
            c[Math.min(revtype, 2)]++;
        }

        List<Raw> result = new ArrayList<>();
        for (Map.Entry<Long, List<ChildChange>> e : byRev.entrySet()) {
            int[] c = counts.get(e.getKey());
            String summary =
                    label
                            + " modified ("
                            + c[0]
                            + " added, "
                            + c[1]
                            + " modified, "
                            + c[2]
                            + " removed)";
            result.add(new Raw(e.getKey(), scope, List.of(summary), e.getValue()));
        }
        return result;
    }

    private Map<Long, RevMeta> revMeta(Set<Long> revs) {
        if (revs.isEmpty()) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows =
                em.createNativeQuery(
                                "SELECT rev, user_id, revtstmp FROM revinfo WHERE rev IN (:revs)")
                        .setParameter("revs", revs)
                        .getResultList();
        Map<Long, RevMeta> meta = new LinkedHashMap<>();
        for (Object[] r : rows) {
            meta.put(
                    num(r[0]).longValue(),
                    new RevMeta(str(r[1]), Instant.ofEpochMilli(num(r[2]).longValue())));
        }
        return meta;
    }

    private static String revLabel(int revtype) {
        return switch (revtype) {
            case 0 -> "ADD";
            case 2 -> "DEL";
            default -> "MOD";
        };
    }

    private static Number num(Object o) {
        return (Number) o;
    }

    private static @Nullable String str(@Nullable Object o) {
        return o == null ? null : o.toString();
    }

    private static @Nullable Integer integer(@Nullable Object o) {
        return o == null ? null : ((Number) o).intValue();
    }

    private static @Nullable BigDecimal bd(@Nullable Object o) {
        if (o == null) {
            return null;
        }
        return o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
    }
}
