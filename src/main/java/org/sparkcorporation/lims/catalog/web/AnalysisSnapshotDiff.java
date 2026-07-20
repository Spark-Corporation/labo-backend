package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Pure field-by-field diff of two {@link AnalysisSnapshot}s, producing the ordered list of changed
 * field names (SP12 config history). {@link BigDecimal} fields compare by value, not scale.
 */
final class AnalysisSnapshotDiff {

    private AnalysisSnapshotDiff() {}

    private record Field(String name, Function<AnalysisSnapshot, Object> get) {}

    private static final List<Field> FIELDS =
            List.of(
                    new Field("name", AnalysisSnapshot::name),
                    new Field("indiciaryValueB", AnalysisSnapshot::indiciaryValueB),
                    new Field("labelCount", AnalysisSnapshot::labelCount),
                    new Field("resultType", AnalysisSnapshot::resultType),
                    new Field("kind", AnalysisSnapshot::kind),
                    new Field("interpretationText", AnalysisSnapshot::interpretationText),
                    new Field("formulaExpression", AnalysisSnapshot::formulaExpression),
                    new Field("defaultPortoir", AnalysisSnapshot::defaultPortoir),
                    new Field("defaultConservationDays", AnalysisSnapshot::defaultConservationDays),
                    new Field("matrixId", AnalysisSnapshot::matrixId),
                    new Field("recipientId", AnalysisSnapshot::recipientId),
                    new Field("units.unit1", AnalysisSnapshot::unit1),
                    new Field("units.unit1Decimals", AnalysisSnapshot::unit1Decimals),
                    new Field("units.unit2", AnalysisSnapshot::unit2),
                    new Field("units.unit2Decimals", AnalysisSnapshot::unit2Decimals),
                    new Field("units.conversionFactor", AnalysisSnapshot::conversionFactor),
                    new Field("delay.days", AnalysisSnapshot::delayDays),
                    new Field("delay.hours", AnalysisSnapshot::delayHours),
                    new Field("delay.anteriorityCount", AnalysisSnapshot::anteriorityCount));

    static List<String> diff(AnalysisSnapshot prev, AnalysisSnapshot curr) {
        List<String> changed = new ArrayList<>();
        for (Field f : FIELDS) {
            if (!equalValue(f.get().apply(prev), f.get().apply(curr))) {
                changed.add(f.name());
            }
        }
        return List.copyOf(changed);
    }

    private static boolean equalValue(Object a, Object b) {
        if (a instanceof BigDecimal ba && b instanceof BigDecimal bb) {
            return ba.compareTo(bb) == 0;
        }
        return Objects.equals(a, b);
    }
}
