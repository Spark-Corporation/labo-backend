package org.sparkcorporation.lims.catalog.domain;

public class IamUserNotFoundException extends RuntimeException {

    public IamUserNotFoundException(String userId) {
        super("Iam User not found: " + userId);
    }
}
