package org.sparkcorporation.lims.iam.web;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
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
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        var resp = service.create(req);
        return ResponseEntity.created(URI.create("/api/v1/admin/users/" + resp.id())).body(resp);
    }

    @GetMapping
    @RequiresPermission("users.manage")
    public UserSearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
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
    public UserResponse get(@PathVariable String id) {
        return service.get(new UserId(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("users.manage")
    public UserResponse update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest req) {
        return service.update(new UserId(id), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("users.manage")
    public void delete(@PathVariable String id) {
        service.delete(new UserId(id));
    }

    @PutMapping("/{id}/roles")
    @RequiresPermission("users.manage")
    public UserResponse assignRoles(
            @PathVariable String id, @Valid @RequestBody AssignRolesRequest req) {
        return service.assignRoles(new UserId(id), req.roles());
    }
}
