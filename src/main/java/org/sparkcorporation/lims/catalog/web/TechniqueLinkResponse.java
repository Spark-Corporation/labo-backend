package org.sparkcorporation.lims.catalog.web;

/** Per-row technique link with default flag. */
public record TechniqueLinkResponse(
        String techniqueId, String code, String name, boolean isDefault) {}
