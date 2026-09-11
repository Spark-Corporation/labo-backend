package org.sparkcorporation.lims.cashbox.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

@Schema(description = "Page of cashbox journal entries matching the requested filters")
public record JournalResponse(
        @Schema(description = "Journal entries on this page, in chronological order")
                List<JournalEntryRow> items,
        @Schema(description = "Zero-based page index") int page,
        @Schema(description = "Requested page size (capped at 200)") int size,
        @Schema(description = "Total number of matching journal entries, across all pages")
                long totalElements) {

    public static JournalResponse from(Page<JournalEntryRow> p) {
        return new JournalResponse(
                p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements());
    }
}
