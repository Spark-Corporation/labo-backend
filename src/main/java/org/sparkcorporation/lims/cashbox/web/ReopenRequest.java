package org.sparkcorporation.lims.cashbox.web;

import jakarta.validation.constraints.NotBlank;

public record ReopenRequest(@NotBlank String reason) {}
