package org.sparkcorporation.lims.catalog.domain;

public class MissingHabilitationException extends RuntimeException {

    public MissingHabilitationException() {
        super("Préleveur requires at least one habilitation");
    }
}
