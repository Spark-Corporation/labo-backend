package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import org.sparkcorporation.lims.catalog.api.AnalysisId;

/**
 * Join entity carrying the (parent_id, child_id, position) tuple under {@code analysis_components}.
 *
 * <p>Owned by the parent {@link Analysis} via {@code @OneToMany}; ordered by {@code position ASC}.
 */
@Entity
@Table(name = "analysis_components")
@IdClass(AnalysisComponentKey.class)
public class AnalysisComponent {

    @Id
    @Column(name = "parent_id", length = 26, nullable = false)
    private String parentId;

    @Id
    @Column(name = "child_id", length = 26, nullable = false)
    private String childId;

    @Column(name = "position", nullable = false)
    private int position;

    protected AnalysisComponent() {}

    public AnalysisComponent(AnalysisId parent, AnalysisId child, int position) {
        this.parentId = parent.value();
        this.childId = child.value();
        this.position = position;
    }

    public String parentId() {
        return parentId;
    }

    public String childId() {
        return childId;
    }

    public int position() {
        return position;
    }

    /** Service uses this to renumber positions after a remove. */
    public void setPosition(int position) {
        this.position = position;
    }
}
