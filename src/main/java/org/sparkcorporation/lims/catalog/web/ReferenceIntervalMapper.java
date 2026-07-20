package org.sparkcorporation.lims.catalog.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.domain.NewReferenceInterval;
import org.sparkcorporation.lims.catalog.domain.ReferenceInterval;

/** Hand-written mapper between {@link ReferenceInterval} domain and web DTOs. */
@Component
public class ReferenceIntervalMapper {

    public NewReferenceInterval toCommand(PutIntervalsRequest.IntervalPayload p) {
        return new NewReferenceInterval(
                p.category(),
                p.sex(),
                p.ageMin(),
                p.ageMax(),
                p.ageUnit(),
                p.mode(),
                p.unit1Min(),
                p.unit1Max(),
                p.unit2Min(),
                p.unit2Max(),
                p.commentText());
    }

    public ReferenceIntervalResponse toResponse(ReferenceInterval ri) {
        return new ReferenceIntervalResponse(
                ri.rawId(),
                ri.rawAnalysisId(),
                ri.category(),
                ri.sex(),
                ri.ageMin(),
                ri.ageMax(),
                ri.ageUnit(),
                ri.mode(),
                ri.unit1Min(),
                ri.unit1Max(),
                ri.unit2Min(),
                ri.unit2Max(),
                ri.commentText(),
                ri.createdAt(),
                ri.updatedAt(),
                ri.version());
    }
}
