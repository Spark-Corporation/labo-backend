package org.sparkcorporation.lims.catalog.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for {@link AnalysisTechniqueLink} — the (analysis_id, technique_id) tuple.
 *
 * <p>Used by JPA via {@code @IdClass}.
 */
public class AnalysisTechniqueLinkKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String analysisId;
    private String techniqueId;

    public AnalysisTechniqueLinkKey() {}

    public AnalysisTechniqueLinkKey(String analysisId, String techniqueId) {
        this.analysisId = analysisId;
        this.techniqueId = techniqueId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getTechniqueId() {
        return techniqueId;
    }

    public void setTechniqueId(String techniqueId) {
        this.techniqueId = techniqueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnalysisTechniqueLinkKey other)) return false;
        return Objects.equals(analysisId, other.analysisId)
                && Objects.equals(techniqueId, other.techniqueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(analysisId, techniqueId);
    }
}
