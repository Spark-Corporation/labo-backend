package org.sparkcorporation.lims.cashbox.web;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.iam.api.UserId;

public record ExpenseFilters(
        @Nullable Instant from,
        @Nullable Instant to,
        @Nullable ExpenseCategoryId categoryId,
        @Nullable UserId recordedBy,
        @Nullable CashRegisterId cashRegisterId) {}
