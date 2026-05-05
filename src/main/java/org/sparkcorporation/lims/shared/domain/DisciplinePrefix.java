package org.sparkcorporation.lims.shared.domain;

/**
 * CDC §IV "Configuration des étiquettes" — the five regulatory disciplines that prefix the daily ID
 * on printed labels. The numeric {@code prefix()} appears as the first character of the daily-ID
 * string (e.g. {@code 1151024-001} for a Biochimie sample). User-configurable disciplines (the
 * {@code catalog.domain.Discipline} aggregate, defined in the catalog sub-project) reference one of
 * these enum values to determine which prefix to apply.
 */
public enum DisciplinePrefix {
    BIOCHIMIE(1, "Biochimie"),
    HEMATOLOGIE(2, "Hématologie"),
    IMMUNO_SEROLOGIE(3, "Immuno-sérologie"),
    MICROBIOLOGIE(4, "Microbiologie"),
    BIOLOGIE_MOLECULAIRE(5, "Biologie moléculaire");

    private final int prefix;
    private final String displayName;

    DisciplinePrefix(int prefix, String displayName) {
        this.prefix = prefix;
        this.displayName = displayName;
    }

    public int prefix() {
        return prefix;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Look up a discipline by its numeric prefix.
     *
     * @throws IllegalArgumentException if {@code prefix} is not in the range {@code 1..5}.
     */
    public static DisciplinePrefix byPrefix(int prefix) {
        for (DisciplinePrefix d : values()) {
            if (d.prefix == prefix) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown DisciplinePrefix: " + prefix);
    }
}
