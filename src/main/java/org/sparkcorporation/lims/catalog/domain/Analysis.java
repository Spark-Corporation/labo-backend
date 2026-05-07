package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.SQLDelete;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;

/**
 * Analysis aggregate per CDC §4 (Bilan société), §8 (Analyses simples), §9 (Analyses groupées). JPA
 * entity with soft delete via Hibernate {@link SQLDelete}.
 *
 * <p>NOTE: deliberately NO {@code @SQLRestriction} — historical FK integrity from 1.5 result lab
 * files is preserved. Repository methods explicitly filter {@code deleted_at IS NULL}.
 *
 * <p>Strict-tree component rule (CDC §9): only {@link AnalysisKind#GROUPED} or {@link
 * AnalysisKind#SOCIETY_PANEL} may have components, and components must themselves be {@link
 * AnalysisKind#SIMPLE}. Self-loops are rejected as a defense-in-depth.
 */
@Entity
@Table(name = "analyses")
@SQLDelete(
        sql =
                "UPDATE analyses SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
public class Analysis {

    @Id
    @Column(length = 26)
    private String id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisKind kind;

    @Column(name = "indiciary_value_b", nullable = false, precision = 10, scale = 2)
    private BigDecimal indiciaryValueB;

    @Column(name = "label_count", nullable = false)
    private int labelCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discipline_id", nullable = false)
    @Nullable
    private Discipline discipline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matrix_id")
    @Nullable
    private Matrix matrix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    @Nullable
    private Recipient recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 10)
    private ResultType resultType;

    @Column(name = "interpretation_text", columnDefinition = "TEXT")
    @Nullable
    private String interpretationText;

    @Column(name = "default_portoir", length = 20)
    @Nullable
    private String defaultPortoir;

    @Column(name = "default_conservation_days")
    @Nullable
    private Integer defaultConservationDays;

    @Embedded private Units units;

    @Embedded private AnalyticalDelay delay;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @OrderBy("position ASC")
    private List<AnalysisComponent> components = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private Set<AnalysisTechniqueLink> techniques = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "deleted_at")
    @Nullable
    private Instant deletedAt;

    protected Analysis() {}

    public static Analysis create(
            NewAnalysis cmd,
            @Nullable Discipline discipline,
            @Nullable Matrix matrix,
            @Nullable Recipient recipient,
            Clock clock) {
        var a = new Analysis();
        a.id = AnalysisId.generate().value();
        a.code = AnalysisCode.of(cmd.code()).value();
        a.name = cmd.name();
        a.kind = cmd.kind();
        a.indiciaryValueB = cmd.indiciaryValueB();
        a.labelCount = cmd.labelCount();
        a.discipline = discipline;
        a.matrix = matrix;
        a.recipient = recipient;
        a.resultType = cmd.resultType();
        a.interpretationText = cmd.interpretationText();
        a.defaultPortoir = cmd.defaultPortoir();
        a.defaultConservationDays = cmd.defaultConservationDays();
        a.units = cmd.units() == null ? Units.empty() : cmd.units();
        a.delay = cmd.delay() == null ? AnalyticalDelay.empty() : cmd.delay();
        var now = Instant.now(clock);
        a.createdAt = now;
        a.updatedAt = now;
        return a;
    }

    /**
     * Mutate this aggregate per the {@link UpdateAnalysis} command. Null fields are ignored. {@code
     * code} and {@code kind} are intentionally absent from the command — both are immutable here;
     * use the convert endpoints to change kind. The matrix/recipient FKs are resolved by the
     * service before this call (passed in as {@link Matrix}/{@link Recipient}, possibly null).
     */
    public void update(
            UpdateAnalysis cmd,
            @Nullable Matrix newMatrix,
            @Nullable Recipient newRecipient,
            Clock clock) {
        if (cmd.name() != null) {
            this.name = cmd.name();
        }
        if (cmd.indiciaryValueB() != null) {
            this.indiciaryValueB = cmd.indiciaryValueB();
        }
        if (cmd.labelCount() != null) {
            this.labelCount = cmd.labelCount();
        }
        if (cmd.matrixId() != null) {
            this.matrix = newMatrix;
        }
        if (cmd.recipientId() != null) {
            this.recipient = newRecipient;
        }
        if (cmd.resultType() != null) {
            this.resultType = cmd.resultType();
        }
        if (cmd.interpretationText() != null) {
            this.interpretationText = cmd.interpretationText();
        }
        if (cmd.defaultPortoir() != null) {
            this.defaultPortoir = cmd.defaultPortoir();
        }
        if (cmd.defaultConservationDays() != null) {
            this.defaultConservationDays = cmd.defaultConservationDays();
        }
        if (cmd.units() != null) {
            this.units = cmd.units();
        }
        if (cmd.delay() != null) {
            this.delay = cmd.delay();
        }
        this.updatedAt = Instant.now(clock);
    }

    /** Add a component child. Strict-tree rule enforced. */
    public void addComponent(Analysis child, int position) {
        if (kind == AnalysisKind.SIMPLE) {
            throw new KindAlreadyMatchesException("cannot add components to a SIMPLE analysis");
        }
        if (this.id.equals(child.id)) {
            throw new ChildMustBeSimpleException("self-loop forbidden");
        }
        if (child.kind != AnalysisKind.SIMPLE) {
            throw new ChildMustBeSimpleException("child must be SIMPLE, got " + child.kind);
        }
        components.add(new AnalysisComponent(this.id(), child.id(), position));
    }

    /** Convert this analysis to GROUPED. Throws if already GROUPED or SOCIETY_PANEL. */
    public void convertToGrouped() {
        if (kind == AnalysisKind.GROUPED || kind == AnalysisKind.SOCIETY_PANEL) {
            throw new KindAlreadyMatchesException("already " + kind);
        }
        kind = AnalysisKind.GROUPED;
    }

    /** Convert this analysis to SIMPLE. Throws if already SIMPLE or if components non-empty. */
    public void convertToSimple() {
        if (kind == AnalysisKind.SIMPLE) {
            throw new KindAlreadyMatchesException("already SIMPLE");
        }
        if (!components.isEmpty()) {
            throw new HasComponentsException("cannot convert to SIMPLE while components exist");
        }
        kind = AnalysisKind.SIMPLE;
    }

    /**
     * Clone this analysis under a new code. Deep-copies components (with new parent_id, same
     * child_ids) and techniques (with new analysis_id). Intervals are NOT cloned here — the service
     * orchestrates that via {@code ReferenceIntervalService.cloneFromSource}.
     */
    public Analysis duplicate(AnalysisCode newCode, Clock clock) {
        var clone = new Analysis();
        clone.id = AnalysisId.generate().value();
        clone.code = newCode.value();
        clone.name = this.name + " (copy)";
        clone.kind = this.kind;
        clone.indiciaryValueB = this.indiciaryValueB;
        clone.labelCount = this.labelCount;
        clone.discipline = this.discipline;
        clone.matrix = this.matrix;
        clone.recipient = this.recipient;
        clone.resultType = this.resultType;
        clone.interpretationText = this.interpretationText;
        clone.defaultPortoir = this.defaultPortoir;
        clone.defaultConservationDays = this.defaultConservationDays;
        clone.units = this.units;
        clone.delay = this.delay;
        for (var comp : this.components) {
            clone.components.add(
                    new AnalysisComponent(
                            clone.id(), AnalysisId.of(comp.childId()), comp.position()));
        }
        for (var t : this.techniques) {
            clone.techniques.add(
                    new AnalysisTechniqueLink(
                            clone.id(),
                            new org.sparkcorporation.lims.catalog.api.TechniqueId(t.techniqueId()),
                            t.isDefault()));
        }
        var now = Instant.now(clock);
        clone.createdAt = now;
        clone.updatedAt = now;
        return clone;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public AnalysisId id() {
        return AnalysisId.of(id);
    }

    public String rawId() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public AnalysisKind kind() {
        return kind;
    }

    public BigDecimal indiciaryValueB() {
        return indiciaryValueB;
    }

    public int labelCount() {
        return labelCount;
    }

    public @Nullable Discipline discipline() {
        return discipline;
    }

    public @Nullable Matrix matrix() {
        return matrix;
    }

    public @Nullable Recipient recipient() {
        return recipient;
    }

    public ResultType resultType() {
        return resultType;
    }

    public @Nullable String interpretationText() {
        return interpretationText;
    }

    public @Nullable String defaultPortoir() {
        return defaultPortoir;
    }

    public @Nullable Integer defaultConservationDays() {
        return defaultConservationDays;
    }

    public Units units() {
        return units;
    }

    public AnalyticalDelay delay() {
        return delay;
    }

    public List<AnalysisComponent> components() {
        return components;
    }

    public Set<AnalysisTechniqueLink> techniques() {
        return techniques;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    public @Nullable Instant deletedAt() {
        return deletedAt;
    }
}
