package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.cashbox.api.SessionStatus;

@Schema(description = "A cash register's session for one business day, with its totals once closed")
public record CashSessionResponse(
        @Schema(description = "ULID of the cash register") String cashRegisterId,
        @Schema(description = "Business date this session covers") LocalDate sessionDate,
        @Schema(description = "Session lifecycle state: OPEN or CLOSED") SessionStatus status,
        @Schema(description = "Total cash payments for the session, in XOF") BigDecimal cashTotal,
        @Schema(description = "Total cheque payments for the session, in XOF")
                BigDecimal chequeTotal,
        @Schema(description = "Total card (TPE) payments for the session, in XOF")
                BigDecimal cardTpeTotal,
        @Schema(description = "Total Flooz mobile-money payments for the session, in XOF")
                BigDecimal floozTotal,
        @Schema(description = "Total Mixx by Yas mobile-money payments for the session, in XOF")
                BigDecimal mixxByYasTotal,
        @Schema(description = "Total expenses recorded against the session, in XOF")
                BigDecimal expensesTotal,
        @Schema(description = "Total home-collection fees recorded against the session, in XOF")
                BigDecimal homeCollectionTotal,
        @Schema(
                        description =
                                "Net total for the session in XOF: sum of all payment modes minus"
                                        + " expenses plus home-collection fees")
                BigDecimal grandTotal,
        @Schema(description = "User id who closed the session, if closed")
                @Nullable String closedBy,
        @Schema(description = "Timestamp the session was closed, if closed")
                @Nullable Instant closedAt,
        @Schema(description = "User id who last reopened the session, if ever reopened")
                @Nullable String reopenedBy,
        @Schema(description = "Timestamp of the last reopening, if ever reopened")
                @Nullable Instant reopenedAt,
        @Schema(description = "Mandatory reason given for the last reopening, if ever reopened")
                @Nullable String reopenReason) {}
