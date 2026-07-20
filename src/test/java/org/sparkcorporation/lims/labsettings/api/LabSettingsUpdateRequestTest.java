package org.sparkcorporation.lims.labsettings.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LabSettingsUpdateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsBlankName() {
        var req = new LabSettingsUpdateRequest("", "address", "acc", "manager");
        Set<ConstraintViolation<LabSettingsUpdateRequest>> violations = validator.validate(req);
        assertThat(propertyPaths(violations)).contains("name");
    }

    @Test
    void rejectsBlankAddress() {
        var req = new LabSettingsUpdateRequest("name", "", "acc", "manager");
        assertThat(propertyPaths(validator.validate(req))).contains("address");
    }

    @Test
    void rejectsBlankAccreditationNumber() {
        var req = new LabSettingsUpdateRequest("name", "address", "", "manager");
        assertThat(propertyPaths(validator.validate(req))).contains("accreditationNumber");
    }

    @Test
    void rejectsBlankManagerName() {
        var req = new LabSettingsUpdateRequest("name", "address", "acc", "");
        assertThat(propertyPaths(validator.validate(req))).contains("managerName");
    }

    @Test
    void acceptsWellFormedRequest() {
        var req =
                new LabSettingsUpdateRequest(
                        "Laboratoire Sparkcorp",
                        "12 Avenue du Test, Lomé",
                        "ACC-2026-001",
                        "Dr. KOFFI Adjoa");
        assertThat(validator.validate(req)).isEmpty();
    }

    private static Stream<String> propertyPaths(
            Set<ConstraintViolation<LabSettingsUpdateRequest>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString());
    }
}
