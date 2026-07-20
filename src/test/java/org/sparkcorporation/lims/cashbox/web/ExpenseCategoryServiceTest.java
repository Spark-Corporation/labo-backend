package org.sparkcorporation.lims.cashbox.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategory;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategoryAlreadyExistsException;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategoryNotFoundException;
import org.sparkcorporation.lims.cashbox.infra.ExpenseCategoryRepository;
import org.sparkcorporation.lims.iam.api.UserId;

@ExtendWith(MockitoExtension.class)
class ExpenseCategoryServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
    private static final UserId BY = new UserId("01KRXY49BFD2BJ9YY1NEFXF8GW");

    @Mock private ExpenseCategoryRepository repo;
    private ExpenseCategoryService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseCategoryService(repo, FIXED);
    }

    @Test
    void createPersistsNewCategory() {
        when(repo.findByCode("UTILITIES")).thenReturn(Optional.empty());
        when(repo.save(any(ExpenseCategory.class))).thenAnswer(a -> a.getArgument(0));

        var response =
                service.create(new ExpenseCategoryRequest("utilities", "Utilities", null), BY);

        assertThat(response.code()).isEqualTo("UTILITIES");
        assertThat(response.label()).isEqualTo("Utilities");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repo.findByCode("UTILITIES"))
                .thenReturn(
                        Optional.of(ExpenseCategory.create("UTILITIES", "Existing", BY, FIXED)));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        new ExpenseCategoryRequest("utilities", "X", null), BY))
                .isInstanceOf(ExpenseCategoryAlreadyExistsException.class);
    }

    @Test
    void updateMutatesAndPersists() {
        var existing = ExpenseCategory.create("X", "Old", BY, FIXED);
        when(repo.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(a -> a.getArgument(0));

        var response =
                service.update(
                        existing.expenseCategoryId(),
                        new ExpenseCategoryRequest(null, "New", false),
                        BY);

        assertThat(response.label()).isEqualTo("New");
        assertThat(response.active()).isFalse();
    }

    @Test
    void updateRejectsMissingId() {
        var missing = ExpenseCategoryId.generate();
        when(repo.findById(missing.value())).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.update(
                                        missing, new ExpenseCategoryRequest(null, "X", null), BY))
                .isInstanceOf(ExpenseCategoryNotFoundException.class);
    }

    @Test
    void deactivateFlipsActiveFalse() {
        var existing = ExpenseCategory.create("X", "Y", BY, FIXED);
        when(repo.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(a -> a.getArgument(0));

        service.deactivate(existing.expenseCategoryId(), BY);

        assertThat(existing.isActive()).isFalse();
    }
}
