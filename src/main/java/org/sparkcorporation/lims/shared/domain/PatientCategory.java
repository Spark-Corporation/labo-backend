package org.sparkcorporation.lims.shared.domain;

import java.util.Objects;

/**
 * CDC reference-intervals "4 catégories de personnes" — the bucket used to pick the right reference
 * interval for a patient based on their {@link Sex} and {@link Age}.
 *
 * <p>Boundary thresholds (set by domain convention; see spec §Open Question 1):
 *
 * <ul>
 *   <li>{@link #NOURRISSON_NOUVEAU_NE} — age &lt; 1 year
 *   <li>{@link #ENFANT} — 1 year ≤ age &lt; 18 years
 *   <li>{@link #HOMME} / {@link #FEMME} — age ≥ 18 years (split by sex)
 * </ul>
 */
public enum PatientCategory {
    HOMME,
    FEMME,
    ENFANT,
    NOURRISSON_NOUVEAU_NE;

    private static final AgeRange NEWBORN = new AgeRange(0, 364, AgeUnit.DAYS);
    private static final AgeRange CHILD =
            new AgeRange(365, 6569, AgeUnit.DAYS); // 1y .. 18y - 1 day

    public static PatientCategory of(Sex sex, Age age) {
        Objects.requireNonNull(sex, "sex");
        Objects.requireNonNull(age, "age");

        if (NEWBORN.contains(age)) {
            return NOURRISSON_NOUVEAU_NE;
        }
        if (CHILD.contains(age)) {
            return ENFANT;
        }
        return sex == Sex.M ? HOMME : FEMME;
    }
}
