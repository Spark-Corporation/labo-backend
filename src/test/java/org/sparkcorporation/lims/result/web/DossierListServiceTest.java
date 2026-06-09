package org.sparkcorporation.lims.result.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.result.infra.DossierRepository;

class DossierListServiceTest {

    private DossierRepository repository;
    private LabFileApi labFileApi;
    private PatientApi patientApi;
    private DossierListService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DossierRepository.class);
        labFileApi = Mockito.mock(LabFileApi.class);
        patientApi = Mockito.mock(PatientApi.class);
        service = new DossierListService(repository, labFileApi, patientApi);

        Mockito.when(
                        repository.list(
                                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));
        Mockito.when(labFileApi.findAllByIds(any())).thenReturn(Map.of());
        Mockito.when(patientApi.findAllByIds(any())).thenReturn(Map.of());
    }

    @Test
    void state_validated_includes_only_fully_bio_validated_dossiers() {
        var filters =
                new DossierFilters(
                        DossierState.VALIDATED, null, null, null, null, null, null, null, null);
        service.list(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .list(
                        isNull(), // includeDeleted
                        isNull(), // deletedOnly
                        isNull(), // urgent
                        isNull(), // empty
                        eq(Boolean.TRUE), // validatedOnly
                        isNull(), // notValidated
                        isNull(), // inProgress
                        isNull(), // printed
                        isNull(), // signed
                        isNull(), // emailed
                        isNull(), // patientId
                        isNull(),
                        isNull(), // dates
                        eq(Pageable.unpaged()));
    }

    @Test
    void printed_signed_emailed_flags_pass_through() {
        var filters =
                new DossierFilters(
                        null,
                        null,
                        null,
                        Boolean.TRUE,
                        Boolean.FALSE,
                        Boolean.TRUE,
                        null,
                        null,
                        null);
        service.list(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .list(
                        isNull(), // includeDeleted
                        isNull(), // deletedOnly
                        isNull(), // urgent
                        isNull(), // empty
                        isNull(), // validatedOnly
                        isNull(), // notValidated
                        isNull(), // inProgress
                        eq(Boolean.TRUE), // printed
                        eq(Boolean.FALSE), // signed
                        eq(Boolean.TRUE), // emailed
                        isNull(), // patientId
                        isNull(),
                        isNull(), // dates
                        eq(Pageable.unpaged()));
    }

    @Test
    void printed_signed_emailed_default_to_null() {
        var filters = new DossierFilters(null, null, null, null, null, null, null, null, null);
        service.list(filters, Pageable.unpaged());

        Mockito.verify(repository)
                .list(
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(), // printed
                        isNull(), // signed
                        isNull(), // emailed
                        isNull(),
                        isNull(),
                        isNull(),
                        eq(Pageable.unpaged()));
    }

    @ParameterizedTest
    @MethodSource("filterCombinations")
    void filter_combinations_map_correctly(
            DossierState state,
            Boolean urgent,
            Boolean empty,
            Boolean expectedValidatedOnly,
            Boolean expectedNotValidated,
            Boolean expectedInProgress,
            Boolean expectedDeletedOnly) {
        var filters = new DossierFilters(state, urgent, empty, null, null, null, null, null, null);
        service.list(filters, Pageable.unpaged());

        var expectedIncludeDeleted = state == DossierState.DELETED ? Boolean.TRUE : null;
        Mockito.verify(repository)
                .list(
                        expectedIncludeDeleted == null ? isNull() : eq(expectedIncludeDeleted),
                        expectedDeletedOnly == null ? isNull() : eq(expectedDeletedOnly),
                        urgent == null ? isNull() : eq(urgent),
                        empty == null ? isNull() : eq(empty),
                        expectedValidatedOnly == null ? isNull() : eq(expectedValidatedOnly),
                        expectedNotValidated == null ? isNull() : eq(expectedNotValidated),
                        expectedInProgress == null ? isNull() : eq(expectedInProgress),
                        isNull(), // printed
                        isNull(), // signed
                        isNull(), // emailed
                        isNull(),
                        isNull(),
                        isNull(),
                        eq(Pageable.unpaged()));
    }

    private static java.util.stream.Stream<Arguments> filterCombinations() {
        return java.util.stream.Stream.of(
                // state=VALIDATED
                Arguments.of(DossierState.VALIDATED, null, null, Boolean.TRUE, null, null, null),
                Arguments.of(
                        DossierState.VALIDATED, Boolean.TRUE, null, Boolean.TRUE, null, null, null),
                Arguments.of(
                        DossierState.VALIDATED, null, Boolean.TRUE, Boolean.TRUE, null, null, null),
                Arguments.of(
                        DossierState.VALIDATED,
                        Boolean.TRUE,
                        Boolean.TRUE,
                        Boolean.TRUE,
                        null,
                        null,
                        null),
                // state=NOT_VALIDATED
                Arguments.of(
                        DossierState.NOT_VALIDATED, null, null, null, Boolean.TRUE, null, null),
                Arguments.of(
                        DossierState.NOT_VALIDATED,
                        Boolean.TRUE,
                        null,
                        null,
                        Boolean.TRUE,
                        null,
                        null),
                Arguments.of(
                        DossierState.NOT_VALIDATED,
                        null,
                        Boolean.TRUE,
                        null,
                        Boolean.TRUE,
                        null,
                        null),
                Arguments.of(
                        DossierState.NOT_VALIDATED,
                        Boolean.TRUE,
                        Boolean.TRUE,
                        null,
                        Boolean.TRUE,
                        null,
                        null),
                // state=IN_PROGRESS
                Arguments.of(DossierState.IN_PROGRESS, null, null, null, null, Boolean.TRUE, null),
                Arguments.of(
                        DossierState.IN_PROGRESS,
                        Boolean.TRUE,
                        null,
                        null,
                        null,
                        Boolean.TRUE,
                        null),
                Arguments.of(
                        DossierState.IN_PROGRESS,
                        null,
                        Boolean.TRUE,
                        null,
                        null,
                        Boolean.TRUE,
                        null),
                Arguments.of(
                        DossierState.IN_PROGRESS,
                        Boolean.TRUE,
                        Boolean.TRUE,
                        null,
                        null,
                        Boolean.TRUE,
                        null),
                // state=DELETED
                Arguments.of(DossierState.DELETED, null, null, null, null, null, Boolean.TRUE),
                Arguments.of(
                        DossierState.DELETED, Boolean.TRUE, null, null, null, null, Boolean.TRUE),
                Arguments.of(
                        DossierState.DELETED, null, Boolean.TRUE, null, null, null, Boolean.TRUE),
                Arguments.of(
                        DossierState.DELETED,
                        Boolean.TRUE,
                        Boolean.TRUE,
                        null,
                        null,
                        null,
                        Boolean.TRUE));
    }
}
