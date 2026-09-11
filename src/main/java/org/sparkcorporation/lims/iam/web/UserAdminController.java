package org.sparkcorporation.lims.iam.web;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.iam.api.UserId;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService service;

    @PostMapping
    @RequiresPermission("users.manage")
    @Operation(
            summary = "Create a user",
            description =
                    "Creates a new staff user account with the given roles. Fails with 409 when the"
                            + " email is already registered.")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        var resp = service.create(req);
        return ResponseEntity.created(URI.create("/api/v1/admin/users/" + resp.id())).body(resp);
    }

    @GetMapping
    @RequiresPermission("users.manage")
    @Operation(
            summary = "Search users",
            description =
                    "Returns a page of staff users, optionally filtered by text query and role.")
    public UserSearchResponse search(
            @Parameter(description = "Free-text filter matched against email and full name.")
                    @RequestParam(required = false)
                    String q,
            @Parameter(description = "Restrict results to users holding this role.")
                    @RequestParam(required = false)
                    String role,
            @Parameter(description = "Zero-based page index.")
                    @RequestParam(defaultValue = "0")
                    @Min(0)
                    int page,
            @Parameter(description = "Page size, 1-100.")
                    @RequestParam(defaultValue = "20")
                    @Min(1)
                    @Max(100)
                    int size) {
        Page<UserResponse> result = service.search(q, role, page, size);
        return new UserSearchResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @GetMapping("/{id}")
    @RequiresPermission("users.manage")
    @Operation(
            summary = "Get a user by id",
            description = "Returns a single staff user. Returns 404 when the id does not exist.")
    public UserResponse get(@Parameter(description = "ULID of the user.") @PathVariable String id) {
        return service.get(new UserId(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("users.manage")
    @Operation(
            summary = "Update a user",
            description =
                    "Partially updates a staff user's email, full name and/or enabled flag. Only"
                            + " supplied fields are changed.")
    public UserResponse update(
            @Parameter(description = "ULID of the user.") @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest req) {
        return service.update(new UserId(id), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("users.manage")
    @Operation(summary = "Delete a user", description = "Permanently deletes a staff user account.")
    public void delete(@Parameter(description = "ULID of the user.") @PathVariable String id) {
        service.delete(new UserId(id));
    }

    @PutMapping("/{id}/roles")
    @RequiresPermission("users.manage")
    @Operation(
            summary = "Replace a user's roles",
            description = "Replaces the full set of roles held by the user with the given set.")
    public UserResponse assignRoles(
            @Parameter(description = "ULID of the user.") @PathVariable String id,
            @Valid @RequestBody AssignRolesRequest req) {
        return service.assignRoles(new UserId(id), req.roles());
    }
}
