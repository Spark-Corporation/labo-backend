package org.sparkcorporation.lims.iam.domain;

/** 409 mapping via iam.web.IamProblemDetailAdvice (handler added in Chunk 6). */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("email already exists: " + email);
    }
}
