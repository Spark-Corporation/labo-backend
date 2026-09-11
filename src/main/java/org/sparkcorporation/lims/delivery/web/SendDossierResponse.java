package org.sparkcorporation.lims.delivery.web;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(
        description =
                "Result of delivering a dossier's results to the patient: the encrypted"
                    + " compte-rendu PDF is emailed, and its password is delivered out-of-band.")
public record SendDossierResponse(
        @Schema(
                        description =
                                "Identifier of the notification enqueued to email the encrypted"
                                        + " compte-rendu PDF.")
                NotificationId emailNotificationId,
        @Schema(
                        description =
                                "Identifier of the notification enqueued to send the password over"
                                        + " WhatsApp, when the patient has a phone number on file.")
                @Nullable NotificationId whatsAppNotificationId,
        @Schema(
                        description =
                                "The compte-rendu decryption password, returned only when the"
                                    + " patient has no phone number on file so staff can relay it"
                                    + " manually. Never logged.")
                @Nullable String password) {}
