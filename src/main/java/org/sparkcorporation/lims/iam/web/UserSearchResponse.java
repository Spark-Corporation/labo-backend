package org.sparkcorporation.lims.iam.web;

import java.util.List;

public record UserSearchResponse(
        List<UserResponse> content, int page, int size, long totalElements, int totalPages) {}
