package org.sparkcorporation.lims.notification.api;

/** Links a notification back to its originating domain object for the ledger (type + ULID). */
public record ContextRef(ContextType type, String id) {}
