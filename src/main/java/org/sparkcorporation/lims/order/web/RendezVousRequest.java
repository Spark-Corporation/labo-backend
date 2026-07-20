package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * PUT {@code /api/v1/lab-files/{id}/rendezvous} payload. A {@code null} {@code rendezVousAt} clears
 * the appointment (this is an explicit set/clear, not the PATCH "no-change" sentinel).
 */
public record RendezVousRequest(@Nullable Instant rendezVousAt) {}
