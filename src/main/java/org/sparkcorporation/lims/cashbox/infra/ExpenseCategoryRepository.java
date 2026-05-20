package org.sparkcorporation.lims.cashbox.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import org.sparkcorporation.lims.cashbox.domain.ExpenseCategory;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, String> {

    Optional<ExpenseCategory> findByCode(String code);

    List<ExpenseCategory> findAllByActive(boolean active);
}
