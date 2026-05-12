package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.result.infra.DossierRepository;

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
}
