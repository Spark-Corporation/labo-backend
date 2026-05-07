package org.sparkcorporation.lims.catalog.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for {@link AnalysisComponent} — the (parent_id, child_id) tuple.
 *
 * <p>Used by JPA via {@code @IdClass}. Field names must match the {@code @Id} fields on the entity.
 */
public class AnalysisComponentKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String parentId;
    private String childId;

    public AnalysisComponentKey() {}

    public AnalysisComponentKey(String parentId, String childId) {
        this.parentId = parentId;
        this.childId = childId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnalysisComponentKey other)) return false;
        return Objects.equals(parentId, other.parentId) && Objects.equals(childId, other.childId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentId, childId);
    }
}
