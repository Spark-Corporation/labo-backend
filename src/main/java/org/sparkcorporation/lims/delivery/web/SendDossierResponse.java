package org.sparkcorporation.lims.delivery.web;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.notification.api.NotificationId;

/**
 * Response body for {@code POST /api/v1/lab-files/{id}:send}. Carries the enqueued notification ids
 * and — <em>only when the patient has no phone</em> — the dossier {@code password} for the staff
 * member to relay manually.
 *
 * <p><strong>The {@code password} field is a secret.</strong> Response bodies for {@code
 * /lab-files/*} are excluded from request/response logging (spec §7); the controller-layer
 * redaction rule keeps it out of log bodies.
 */
public record SendDossierResponse(
        NotificationId emailNotificationId,
        @Nullable NotificationId whatsAppNotificationId,
        @Nullable String password) {}
