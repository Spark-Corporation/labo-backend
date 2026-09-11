package org.sparkcorporation.lims.cashbox.web;

import java.util.List;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(
            summary = "List expense categories",
            description =
                    "Returns expense categories, optionally filtered to only active or only"
                            + " inactive ones; omit the filter to return all.")
    public List<ExpenseCategoryResponse> list(
            @RequestParam
                    @Parameter(
                            description =
                                    "Filter on the active flag; omit to return all categories")
                    @Nullable Boolean active) {
        return service.list(active);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("cashbox.expense-category.manage")
    @Operation(
            summary = "Create an expense category",
            description =
                    "Creates a new expense category with a unique, uppercase-normalized code and a"
                        + " label; active defaults to true. Rejected with a conflict if the code is"
                        + " already in use.")
    public ExpenseCategoryResponse create(@Valid @RequestBody ExpenseCategoryRequest req) {
        return service.create(req, callerId());
    }

    @PatchMapping("/{id}")
    @RequiresPermission("cashbox.expense-category.manage")
    @Operation(
            summary = "Update an expense category",
            description =
                    "Updates the label and/or active flag of an existing category; the code is"
                            + " immutable and any code supplied in the body is ignored. Fields left"
                            + " null are unchanged.")
    public ExpenseCategoryResponse update(
            @PathVariable @Parameter(description = "ULID of the expense category") String id,
            @Valid @RequestBody ExpenseCategoryRequest req) {
        return service.update(new ExpenseCategoryId(id), req, callerId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("cashbox.expense-category.manage")
    @Operation(
            summary = "Deactivate an expense category",
            description =
                    "Soft-deletes the category by flipping active to false; the row is kept so"
                            + " historical expense references remain valid.")
    public void delete(
            @PathVariable @Parameter(description = "ULID of the expense category") String id) {
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
