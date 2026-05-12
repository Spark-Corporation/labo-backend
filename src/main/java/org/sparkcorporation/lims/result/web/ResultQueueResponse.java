package org.sparkcorporation.lims.result.web;

import java.util.List;

import org.springframework.data.domain.Page;

import org.sparkcorporation.lims.result.api.ResultSummary;

public record ResultQueueResponse(
        List<ResultSummary> content, int page, int size, long totalElements, int totalPages) {

    public ResultQueueResponse {
        content = List.copyOf(content);
    }

    public static ResultQueueResponse from(Page<ResultSummary> page) {
        return new ResultQueueResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
