package org.sparkcorporation.lims.iam.web;

import jakarta.validation.Valid;

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
    public UserResponse me() {
        return service.getCurrent();
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        service.changePassword(new UserId(auth.getName()), req);
    }
}
