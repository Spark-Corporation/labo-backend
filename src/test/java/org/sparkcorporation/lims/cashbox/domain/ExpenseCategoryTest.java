package org.sparkcorporation.lims.cashbox.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.iam.api.UserId;

class ExpenseCategoryTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
    private static final UserId BY = new UserId("01KRXY49BFD2BJ9YY1NEFXF8GW");

    @Test
    void createNormalizesCodeToUpperAndIsActiveByDefault() {
        var cat = ExpenseCategory.create("utilities", "Utilities", BY, FIXED);

        assertThat(cat.getCode()).isEqualTo("UTILITIES");
        assertThat(cat.getLabel()).isEqualTo("Utilities");
        assertThat(cat.isActive()).isTrue();
        assertThat(cat.getCreatedAt()).isEqualTo(FIXED.instant());
    }

    @Test
    void createRejectsInvalidCode() {
        assertThatThrownBy(() -> ExpenseCategory.create("with space", "x", BY, FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code");
        assertThatThrownBy(() -> ExpenseCategory.create("", "x", BY, FIXED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpenseCategory.create("A".repeat(33), "x", BY, FIXED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsBlankLabel() {
        assertThatThrownBy(() -> ExpenseCategory.create("X", "  ", BY, FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label");
    }

    @Test
    void updateLabelTouchesUpdatedAtNotCreatedAt() {
        var cat = ExpenseCategory.create("X", "Old", BY, FIXED);
        Clock later = Clock.fixed(Instant.parse("2026-05-18T11:00:00Z"), ZoneOffset.UTC);

        cat.updateLabel("New", BY, later);

        assertThat(cat.getLabel()).isEqualTo("New");
        assertThat(cat.getCreatedAt()).isEqualTo(FIXED.instant());
        assertThat(cat.getUpdatedAt()).isEqualTo(later.instant());
    }

    @Test
    void setActiveFlipsTheFlag() {
        var cat = ExpenseCategory.create("X", "Y", BY, FIXED);
        cat.setActive(false, BY, FIXED);
        assertThat(cat.isActive()).isFalse();
        cat.setActive(true, BY, FIXED);
        assertThat(cat.isActive()).isTrue();
    }
}
