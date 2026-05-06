package org.sparkcorporation.lims.iam.api;

import java.util.Set;

public record UserSummary(
        UserId id, String email, String fullName, boolean enabled, Set<Role> roles) {}
