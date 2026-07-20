package org.sparkcorporation.lims.cashbox.web;

import java.util.List;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.iam.api.UserId;

@RestController
@RequestMapping("/api/v1/cashbox/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

    private final ExpenseCategoryService service;

    @GetMapping
    @RequiresPermission("cashbox.expense-category.read")
    public List<ExpenseCategoryResponse> list(@RequestParam @Nullable Boolean active) {
        return service.list(active);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("cashbox.expense-category.manage")
    public ExpenseCategoryResponse create(@Valid @RequestBody ExpenseCategoryRequest req) {
        return service.create(req, callerId());
    }

    @PatchMapping("/{id}")
    @RequiresPermission("cashbox.expense-category.manage")
    public ExpenseCategoryResponse update(
            @PathVariable String id, @Valid @RequestBody ExpenseCategoryRequest req) {
        return service.update(new ExpenseCategoryId(id), req, callerId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("cashbox.expense-category.manage")
    public void delete(@PathVariable String id) {
        service.deactivate(new ExpenseCategoryId(id), callerId());
    }

    private static UserId callerId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("no authenticated user in SecurityContext");
        }
        return new UserId(auth.getName());
    }
}
