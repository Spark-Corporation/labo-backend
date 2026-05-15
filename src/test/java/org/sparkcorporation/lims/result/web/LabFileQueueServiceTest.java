package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.result.infra.DossierRepository;
import org.sparkcorporation.lims.result.infra.OverdueDossierRowProjection;

class LabFileQueueServiceTest {

    private DossierRepository repository;
    private LabFileApi labFileApi;
    private PatientApi patientApi;
    private LabFileQueueService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DossierRepository.class);
        labFileApi = Mockito.mock(LabFileApi.class);
        patientApi = Mockito.mock(PatientApi.class);
        service = new LabFileQueueService(repository, labFileApi, patientApi);
    }

    @Test
    void empty_page_hydrates_no_cross_module_calls() {
        Mockito.when(repository.findUrgent(Mockito.any())).thenReturn(new PageImpl<>(List.of()));
        Mockito.when(labFileApi.findAllByIds(java.util.Set.of())).thenReturn(Map.of());
        Mockito.when(patientApi.findAllByIds(java.util.Set.of())).thenReturn(Map.of());

        var page = service.findUrgent(PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void findOverdue_batches_labfile_and_patient_lookups_once() {
        var p1 = mockProjection(LAB_1, PAT_1);
        var p2 = mockProjection(LAB_2, PAT_1); // same patient → still batched
        when(repository.findOverdue(any())).thenReturn(new PageImpl<>(List.of(p1, p2)));
        when(labFileApi.findAllByIds(anySet())).thenReturn(Map.of());
        when(patientApi.findAllByIds(anySet())).thenReturn(Map.of());

        var page = service.findOverdue(PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(2);
        verify(labFileApi, times(1)).findAllByIds(anySet());
        verify(patientApi, times(1)).findAllByIds(anySet());
    }

    @Test
    void findOverdue_hydrates_inner_dossier_block_from_labfile_and_patient_summaries() {
        var projection = mockProjection(LAB_1, PAT_1);
        when(repository.findOverdue(any())).thenReturn(new PageImpl<>(List.of(projection)));

        LabFileSummary labFile = Mockito.mock(LabFileSummary.class, Mockito.RETURNS_DEEP_STUBS);
        when(labFile.registrationNumber().value()).thenReturn("LF-260514-0042");
        when(labFile.createdAt()).thenReturn(Instant.parse("2026-05-12T08:14:00Z"));
        when(labFile.urgent()).thenReturn(false);
        when(labFileApi.findAllByIds(anySet())).thenReturn(Map.of(new LabFileId(LAB_1), labFile));

        PatientSummary patient = Mockito.mock(PatientSummary.class);
        when(patient.labCode()).thenReturn("LAB-260512-0007");
        when(patient.lastName()).thenReturn("Doe");
        when(patient.firstNames()).thenReturn("Jane");
        when(patient.insurance()).thenReturn(null);
        when(patientApi.findAllByIds(anySet())).thenReturn(Map.of(new PatientId(PAT_1), patient));

        var page = service.findOverdue(PageRequest.of(0, 20));

        var row = page.getContent().get(0);
        assertThat(row.dossier().labFileId()).isEqualTo(LAB_1);
        assertThat(row.dossier().registrationNumber()).isEqualTo("LF-260514-0042");
        assertThat(row.dossier().patientLastName()).isEqualTo("Doe");
        assertThat(row.overdue().analysisCode()).isEqualTo("GLY");
        assertThat(row.overdue().hoursPastDeadline()).isEqualTo(6);
        assertThat(row.overdue().overdueAnalysisCount()).isEqualTo(1);
    }

    private static final String LAB_1 = "01HZX000000000000000000001";
    private static final String LAB_2 = "01HZX000000000000000000002";
    private static final String PAT_1 = "01HZY000000000000000000001";

    private OverdueDossierRowProjection mockProjection(String labFileId, String patientId) {
        var p = Mockito.mock(OverdueDossierRowProjection.class);
        when(p.getLabFileId()).thenReturn(labFileId);
        when(p.getPatientId()).thenReturn(patientId);
        when(p.getAnalysisCode()).thenReturn("GLY");
        when(p.getAnalysisName()).thenReturn("Glycemie");
        when(p.getDeadline()).thenReturn(Instant.parse("2026-05-14T08:00:00Z"));
        when(p.getHoursPastDeadline()).thenReturn(6);
        when(p.getOverdueAnalysisCount()).thenReturn(1);
        when(p.getMarker()).thenReturn("PEN");
        return p;
    }
}
