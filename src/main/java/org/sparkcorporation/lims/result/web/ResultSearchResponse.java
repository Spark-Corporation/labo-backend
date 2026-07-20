package org.sparkcorporation.lims.result.web;

import java.util.List;

import org.springframework.data.domain.Page;

public record ResultSearchResponse(
        List<ResultSearchRow> content, int page, int size, long totalElements, int totalPages) {

    public ResultSearchResponse {
        content = List.copyOf(content);
    }

    public static ResultSearchResponse from(Page<ResultSearchRow> page) {
        return new ResultSearchResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
