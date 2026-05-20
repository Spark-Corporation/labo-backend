package org.sparkcorporation.lims.cashbox.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.cashbox.domain.ExpenseCategory;
import org.sparkcorporation.lims.iam.api.UserId;

class ExpenseCategoryMapperTest {
    @Test
    void mapsAllFields() {
        var fixed = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
        var by = new UserId("01KRXY49BFD2BJ9YY1NEFXF8GW");
        var domain = ExpenseCategory.create("X", "label", by, fixed);

        var response = ExpenseCategoryMapper.toResponse(domain);

        assertThat(response.id()).isEqualTo(domain.getId());
        assertThat(response.code()).isEqualTo("X");
        assertThat(response.label()).isEqualTo("label");
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(fixed.instant());
    }
}
