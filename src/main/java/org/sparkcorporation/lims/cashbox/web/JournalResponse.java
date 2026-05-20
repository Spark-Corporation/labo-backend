package org.sparkcorporation.lims.cashbox.web;

import java.util.List;

import org.springframework.data.domain.Page;

public record JournalResponse(List<JournalEntryRow> items, int page, int size, long totalElements) {

    public static JournalResponse from(Page<JournalEntryRow> p) {
        return new JournalResponse(
                p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements());
    }
}
