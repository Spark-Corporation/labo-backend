package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.shared.domain.Sex;

class LabFileMapperRendezVousTest {

    private static final String ULID = "01HZB0000000000000000ADMNZ";

    private static LabFileSummary summaryWith(Instant rendezVousAt) {
        // LabFileSummary(id, registrationNumber, patient, prescriber, preleveur, urgent, status,
        //   samples, analyses, insurance, partner, billing, createdAt, createdByUserId, signedBy,
        //   signedAt, rendezVousAt)
        return new LabFileSummary(
                new LabFileId(ULID),
                new LabFileRegistrationNumber("LF-260622-0001"),
                new LabFileSummary.PatientSnapshot(ULID, "P001", "Doe", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                List.of(),
                List.of(),
                null,
                null,
                new LabFileSummary.BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                Instant.parse("2026-06-22T10:00:00Z"),
                null,
                null,
                null,
                rendezVousAt);
    }

    @Test
    void to_response_copies_rendezvous() {
        var when = Instant.parse("2026-06-25T14:30:00Z");
        var resp = LabFileMapper.toResponse(summaryWith(when));
        assertThat(resp.rendezVousAt()).isEqualTo(when);
    }

    @Test
    void to_response_allows_null_rendezvous() {
        var resp = LabFileMapper.toResponse(summaryWith(null));
        assertThat(resp.rendezVousAt()).isNull();
    }
}
