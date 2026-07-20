package org.sparkcorporation.lims.catalog.web;

/** Per-row component link from a GROUPED/SOCIETY_PANEL parent to a SIMPLE child. */
public record ComponentLinkResponse(
        String childId, String childCode, String childName, int position) {}
