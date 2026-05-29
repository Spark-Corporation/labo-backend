package org.sparkcorporation.lims.notification.api;

/** Exactly one optional attachment per message (e.g. a quote PDF or an encrypted result PDF). */
public record Attachment(String filename, String contentType, byte[] bytes) {}
