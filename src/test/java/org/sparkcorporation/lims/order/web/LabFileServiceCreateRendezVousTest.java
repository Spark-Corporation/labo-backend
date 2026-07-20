package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.sparkcorporation.lims.order.infra.DefaultLabFileRegistrationGenerator;
import org.sparkcorporation.lims.order.infra.LabFileRepository;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

class LabFileServiceCreateRendezVousTest {

    private static final String ULID = "01HZB0000000000000000ADMNZ";
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-22T10:00:00Z"), ZoneOffset.UTC);

    private final LabFileRepository fileRepo = mock(LabFileRepository.class);
    private final LabFileApi labFileApi = mock(LabFileApi.class);
    private final PatientApi patientApi = mock(PatientApi.class);
    private final DefaultLabFileRegistrationGenerator regGen =
            mock(DefaultLabFileRegistrationGenerator.class);

    private LabFileService newService() {
        return new LabFileService(
                fileRepo,
                labFileApi,
                patientApi,
                mock(PrescriberApi.class),
                mock(PreleveurApi.class),
                mock(InsuranceApi.class),
                mock(PartnerApi.class),
                mock(AnalysisApi.class),
                regGen,
                mock(ApplicationEventPublisher.class),
                CLOCK);
    }

    private static PatientSummary patient() {
        return new PatientSummary(
                new PatientId(ULID),
                "P001",
                "Doe",
                "Jane",
                Civility.MADAME,
                Sex.F,
                null,
                null,
                null,
                false,
                false);
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
                new LabFileSummary.BillingBlock(
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, null, null),
                Instant.parse("2026-06-22T10:00:00Z"),
                null,
                null,
                null,
                null);
    }

    @Test
    void create_threads_rendezvous_onto_the_opened_file() {
        when(patientApi.findById(new PatientId(ULID))).thenReturn(Optional.of(patient()));
        when(regGen.next(any(LocalDate.class)))
                .thenReturn(new LabFileRegistrationNumber("LF-260622-0001"));
        when(labFileApi.requireById(any(LabFileId.class))).thenReturn(minimalSummary());
        var svc = newService();
        var rdv = Instant.parse("2026-06-25T14:30:00Z");
        // CreateLabFileRequest(patientId, prescriberId, preleveurId, urgent, analyses, insuranceId,
        //   partnerId, homeCollectionFee, additionalFees, discountKind, discountValue,
        // rendezVousAt)
        var req =
                new CreateLabFileRequest(
                        ULID, null, null, false, List.of(), null, null, null, null, null, null,
                        rdv);

        svc.create(req, "user-1");

        var captor = ArgumentCaptor.forClass(LabFile.class);
        verify(fileRepo).save(captor.capture());
        assertThat(captor.getValue().getRendezVousAt()).isEqualTo(rdv);
    }
}
