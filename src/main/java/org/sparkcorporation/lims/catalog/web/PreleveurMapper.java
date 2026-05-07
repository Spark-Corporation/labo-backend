package org.sparkcorporation.lims.catalog.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.domain.NewPreleveur;
import org.sparkcorporation.lims.catalog.domain.Preleveur;
import org.sparkcorporation.lims.catalog.domain.UpdatePreleveur;

/** Hand-written mapper between {@link Preleveur} domain and web DTOs. */
@Component
public class PreleveurMapper {

    public NewPreleveur toCommand(CreatePreleveurRequest req) {
        return new NewPreleveur(
                req.name(), req.phone(), req.email(), req.userId(), req.habilitations());
    }

    public UpdatePreleveur toCommand(UpdatePreleveurRequest req) {
        return new UpdatePreleveur(
                req.name(), req.phone(), req.email(), req.userId(), req.habilitations());
    }

    public PreleveurResponse toResponse(Preleveur p) {
        return new PreleveurResponse(
                p.rawId(),
                p.code(),
                p.name(),
                p.phone(),
                p.email(),
                p.userId(),
                p.habilitations(),
                p.createdAt(),
                p.updatedAt(),
                p.version());
    }
}
