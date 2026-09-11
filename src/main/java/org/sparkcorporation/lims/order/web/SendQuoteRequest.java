package org.sparkcorporation.lims.order.web;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.notification.api.Channel;

/** Request body for {@code POST /api/v1/quotes/{id}:send} — the channels to send the quote over. */
@Schema(
        description =
                "Request to send a locked quote to the patient over one or more notification"
                        + " channels.")
public record SendQuoteRequest(
        @Schema(
                        description =
                                "Notification channels to send the quote over (EMAIL, WHATSAPP)."
                                        + " Must not be empty.")
                @NotEmpty
                Set<Channel> channels) {}
