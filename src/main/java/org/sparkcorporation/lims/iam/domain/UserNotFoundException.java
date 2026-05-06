package org.sparkcorporation.lims.iam.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.iam.api.UserId;

/** 404 mapping via shared.web.ProblemDetailAdvice. */
public class UserNotFoundException extends EntityNotFoundException {

    public UserNotFoundException(UserId id) {
        super("user not found: " + id.value());
    }
}
