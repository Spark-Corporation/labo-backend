package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.patient.api.PatientId;

class PatientNotFoundExceptionTest {

    @Test
    void is_an_EntityNotFoundException_so_ProblemDetailAdvice_maps_to_404() {
        var ex = new PatientNotFoundException(PatientId.next());
        assertThat(ex).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void message_includes_the_id() {
        var id = PatientId.next();
        var ex = new PatientNotFoundException(id);
        assertThat(ex.getMessage()).contains(id.value());
    }
}
