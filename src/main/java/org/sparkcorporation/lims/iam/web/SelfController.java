package org.sparkcorporation.lims.iam.web;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.iam.api.UserId;

/** Self-service: profile + password change. ⚠ security surface — see spec §8.8. */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class SelfController {

    private final UserService service;

    @GetMapping
    @Operation(
            summary = "Get the current user's profile",
            description = "Returns the profile of the currently authenticated user.")
    public UserResponse me() {
        return service.getCurrent();
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Change the current user's password",
            description =
                    "Changes the currently authenticated user's own password after verifying the"
                            + " supplied old password.")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        service.changePassword(new UserId(auth.getName()), req);
    }
}
