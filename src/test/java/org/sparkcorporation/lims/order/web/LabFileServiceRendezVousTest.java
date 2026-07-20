package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.InsuranceApi;
import org.sparkcorporation.lims.catalog.api.PartnerApi;
import org.sparkcorporation.lims.catalog.api.PreleveurApi;
import org.sparkcorporation.lims.catalog.api.PrescriberApi;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.domain.LabFile;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.NewLabFile;
import org.sparkcorporation.lims.order.infra.DefaultLabFileRegistrationGenerator;
import org.sparkcorporation.lims.order.infra.LabFileRepository;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.shared.domain.Sex;

class LabFileServiceRendezVousTest {

    private static final String ULID = "01HZB0000000000000000ADMNZ";
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-22T10:00:00Z"), ZoneOffset.UTC);

    private final LabFileRepository fileRepo = mock(LabFileRepository.class);
    private final LabFileApi labFileApi = mock(LabFileApi.class);

    private LabFileService newService() {
        return new LabFileService(
                fileRepo,
                labFileApi,
                mock(PatientApi.class),
                mock(PrescriberApi.class),
                mock(PreleveurApi.class),
                mock(InsuranceApi.class),
                mock(PartnerApi.class),
                mock(AnalysisApi.class),
                mock(DefaultLabFileRegistrationGenerator.class),
                mock(ApplicationEventPublisher.class),
                CLOCK);
    }

    private static LabFile openFile() {
        var cmd =
                new NewLabFile(
                        ULID,
                        null,
                        null,
                        false,
                        List.of(),
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null);
        return LabFile.open(cmd, new LabFileRegistrationNumber("LF-260622-0001"), "user-1", CLOCK);
    }

    private static LabFileSummary minimalSummary() {
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
                Instant.parse("2026-06-25T14:30:00Z"));
    }

    @Test
    void schedule_loads_mutates_and_returns_response() {
        var file = openFile();
        when(fileRepo.findActiveById(ULID)).thenReturn(Optional.of(file));
        when(labFileApi.requireById(new LabFileId(ULID))).thenReturn(minimalSummary());
        var svc = newService();
        var when = Instant.parse("2026-06-25T14:30:00Z");

        var resp = svc.scheduleRendezVous(new LabFileId(ULID), when);

        assertThat(file.getRendezVousAt()).isEqualTo(when);
        assertThat(resp.rendezVousAt()).isEqualTo(when);
    }

    @Test
    void schedule_unknown_throws_404_type() {
        when(fileRepo.findActiveById(ULID)).thenReturn(Optional.empty());
        var svc = newService();

        assertThatThrownBy(
                        () ->
                                svc.scheduleRendezVous(
                                        new LabFileId(ULID), Instant.parse("2026-06-25T14:30:00Z")))
                .isInstanceOf(LabFileNotFoundException.class);
    }
}
