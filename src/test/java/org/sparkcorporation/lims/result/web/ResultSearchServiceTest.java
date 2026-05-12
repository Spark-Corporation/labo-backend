package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.result.infra.ResultSearchRepository;

class ResultSearchServiceTest {

    private ResultSearchRepository repository;
    private PatientApi patientApi;
    private ResultSearchService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ResultSearchRepository.class);
        patientApi = Mockito.mock(PatientApi.class);
        service = new ResultSearchService(repository, patientApi);
    }

    @Test
    void date_range_with_inverted_bounds_throws_illegal_argument() {
        SearchFilters filters =
                new SearchFilters(
                        LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1), null, null, null);

        assertThatThrownBy(() -> service.search(filters, Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFrom")
                .hasMessageContaining("dateTo");
    }

    @Test
    void null_filters_pass_all_nulls_through_to_repository() {
        SearchFilters filters = new SearchFilters(null, null, null, null, null);
        Mockito.when(repository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        Mockito.when(patientApi.findAllByIds(Mockito.anySet())).thenReturn(Map.of());

        service.search(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .search(null, null, null, null, null, null, null, Pageable.unpaged());
    }

    @Test
    void urgent_result_state_maps_to_urgentOnly_predicate() {
        SearchFilters filters = new SearchFilters(null, null, null, SearchResultState.URGENT, null);
        Mockito.when(repository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        Mockito.when(patientApi.findAllByIds(Mockito.anySet())).thenReturn(Map.of());

        service.search(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .search(null, null, null, null, null, null, Boolean.TRUE, Pageable.unpaged());
    }

    @Test
    void insured_patient_type_maps_to_insuredFilter_true() {
        SearchFilters filters = new SearchFilters(null, null, PatientType.INSURED, null, null);
        Mockito.when(repository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        Mockito.when(patientApi.findAllByIds(Mockito.anySet())).thenReturn(Map.of());

        service.search(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .search(null, null, null, Boolean.TRUE, null, null, null, Pageable.unpaged());
    }

    @Test
    void validated_state_maps_to_state_equals_biological_validated() {
        SearchFilters filters =
                new SearchFilters(null, null, null, SearchResultState.VALIDATED, null);
        Mockito.when(repository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        Mockito.when(patientApi.findAllByIds(Mockito.anySet())).thenReturn(Map.of());

        service.search(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .search(
                        null,
                        null,
                        null,
                        null,
                        "BIOLOGICAL_VALIDATED",
                        null,
                        null,
                        Pageable.unpaged());
    }

    @Test
    void not_insured_patient_type_maps_to_insuredFilter_false() {
        SearchFilters filters = new SearchFilters(null, null, PatientType.NOT_INSURED, null, null);
        Mockito.when(repository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        Mockito.when(patientApi.findAllByIds(Mockito.anySet())).thenReturn(Map.of());

        service.search(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .search(
                        isNull(),
                        isNull(),
                        isNull(),
                        Mockito.eq(Boolean.FALSE),
                        isNull(),
                        isNull(),
                        isNull(),
                        Mockito.eq(Pageable.unpaged()));
    }

    @Test
    void empty_state_maps_to_state_equals_draft() {
        SearchFilters filters = new SearchFilters(null, null, null, SearchResultState.EMPTY, null);
        Mockito.when(repository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        Mockito.when(patientApi.findAllByIds(Mockito.anySet())).thenReturn(Map.of());

        service.search(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .search(
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        Mockito.eq("DRAFT"),
                        isNull(),
                        isNull(),
                        Mockito.eq(Pageable.unpaged()));
    }

    @Test
    void to_check_state_maps_to_state_equals_entered() {
        SearchFilters filters =
                new SearchFilters(null, null, null, SearchResultState.TO_CHECK, null);
        Mockito.when(repository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        Mockito.when(patientApi.findAllByIds(Mockito.anySet())).thenReturn(Map.of());

        service.search(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .search(
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        Mockito.eq("ENTERED"),
                        isNull(),
                        isNull(),
                        Mockito.eq(Pageable.unpaged()));
    }

    @Test
    void not_validated_state_maps_to_notValidated_true() {
        SearchFilters filters =
                new SearchFilters(null, null, null, SearchResultState.NOT_VALIDATED, null);
        Mockito.when(repository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        Mockito.when(patientApi.findAllByIds(Mockito.anySet())).thenReturn(Map.of());

        service.search(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .search(
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        Mockito.eq(Boolean.TRUE),
                        isNull(),
                        Mockito.eq(Pageable.unpaged()));
    }

    @Test
    void non_null_date_range_passes_start_and_end_of_day_to_repository() {
        var dateFrom = LocalDate.of(2026, 5, 1);
        var dateTo = LocalDate.of(2026, 5, 10);
        SearchFilters filters = new SearchFilters(dateFrom, dateTo, null, null, null);
        Mockito.when(repository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        Mockito.when(patientApi.findAllByIds(Mockito.anySet())).thenReturn(Map.of());

        service.search(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .search(
                        notNull(),
                        notNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        Mockito.eq(Pageable.unpaged()));
    }

    @Test
    void only_date_from_set_passes_start_of_day_and_null_end() {
        var dateFrom = LocalDate.of(2026, 5, 1);
        SearchFilters filters = new SearchFilters(dateFrom, null, null, null, null);
        Mockito.when(repository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        Mockito.when(patientApi.findAllByIds(Mockito.anySet())).thenReturn(Map.of());

        service.search(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .search(
                        notNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        Mockito.eq(Pageable.unpaged()));
    }

    @Test
    void only_date_to_set_passes_null_start_and_end_of_day() {
        var dateTo = LocalDate.of(2026, 5, 10);
        SearchFilters filters = new SearchFilters(null, dateTo, null, null, null);
        Mockito.when(repository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        Mockito.when(patientApi.findAllByIds(Mockito.anySet())).thenReturn(Map.of());

        service.search(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .search(
                        isNull(),
                        notNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        Mockito.eq(Pageable.unpaged()));
    }
}
