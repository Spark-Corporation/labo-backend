package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.TechniqueId;

/**
 * Join entity for {@code analysis_techniques(analysis_id, technique_id, is_default)}.
 *
 * <p>{@code is_default} is the payload that motivates this entity rather than a
 * {@code @ManyToMany}. At most one link per {@link Analysis} may have {@code isDefault = true};
 * enforcement at the service layer (see {@code MultipleDefaultTechniquesException}).
 */
@Entity
@Table(name = "analysis_techniques")
@Audited
@IdClass(AnalysisTechniqueLinkKey.class)
public class AnalysisTechniqueLink {

    @Id
    @Column(name = "analysis_id", length = 26, nullable = false)
    private String analysisId;

    @Id
    @Column(name = "technique_id", length = 26, nullable = false)
    private String techniqueId;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    protected AnalysisTechniqueLink() {}

    public AnalysisTechniqueLink(
            AnalysisId analysisId, TechniqueId techniqueId, boolean isDefault) {
        this.analysisId = analysisId.value();
        this.techniqueId = techniqueId.value();
        this.isDefault = isDefault;
    }

    public String analysisId() {
        return analysisId;
    }

    public String techniqueId() {
        return techniqueId;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
