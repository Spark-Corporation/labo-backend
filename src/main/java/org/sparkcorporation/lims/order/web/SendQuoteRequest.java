package org.sparkcorporation.lims.order.web;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

import org.sparkcorporation.lims.notification.api.Channel;

/** Request body for {@code POST /api/v1/quotes/{id}:send} — the channels to send the quote over. */
public record SendQuoteRequest(@NotEmpty Set<Channel> channels) {}
