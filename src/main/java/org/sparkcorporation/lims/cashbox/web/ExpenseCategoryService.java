package org.sparkcorporation.lims.cashbox.web;

import java.time.Clock;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategory;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategoryAlreadyExistsException;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategoryNotFoundException;
import org.sparkcorporation.lims.cashbox.infra.ExpenseCategoryRepository;
import org.sparkcorporation.lims.iam.api.UserId;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository repo;
    private final Clock clock;

    @Transactional
    public ExpenseCategoryResponse create(ExpenseCategoryRequest req, UserId by) {
        if (req.code() == null) {
            throw new IllegalArgumentException("code is required for create");
        }
        String normalized = req.code().toUpperCase();
        repo.findByCode(normalized)
                .ifPresent(
                        c -> {
                            throw new ExpenseCategoryAlreadyExistsException(normalized);
                        });
        var created = ExpenseCategory.create(normalized, req.label(), by, clock);
        return ExpenseCategoryMapper.toResponse(repo.save(created));
    }

    @Transactional
    public ExpenseCategoryResponse update(
            ExpenseCategoryId id, ExpenseCategoryRequest req, UserId by) {
        var cat =
                repo.findById(id.value())
                        .orElseThrow(() -> new ExpenseCategoryNotFoundException(id));
        if (req.label() != null) {
            cat.updateLabel(req.label(), by, clock);
        }
        if (req.active() != null) {
            cat.setActive(req.active(), by, clock);
        }
        return ExpenseCategoryMapper.toResponse(repo.save(cat));
    }

    @Transactional
    public void deactivate(ExpenseCategoryId id, UserId by) {
        var cat =
                repo.findById(id.value())
                        .orElseThrow(() -> new ExpenseCategoryNotFoundException(id));
        cat.setActive(false, by, clock);
        repo.save(cat);
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> list(Boolean active) {
        var rows = (active == null) ? repo.findAll() : repo.findAllByActive(active);
        return rows.stream().map(ExpenseCategoryMapper::toResponse).toList();
    }
}
