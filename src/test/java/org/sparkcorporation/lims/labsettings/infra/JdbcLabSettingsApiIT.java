package org.sparkcorporation.lims.labsettings.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.labsettings.api.LabSettingsUpdateRequest;

// Tests share the JVM-scoped Testcontainers PostgreSQL singleton; mutations from {@code
// update_*} persist into subsequent tests. Lock method order so {@code get_returnsSeededRow}
// runs against the pristine Liquibase-seeded row before {@code update_*} mutates it. @AfterEach
// also restores the seed values so subsequent IT classes inherit a known state — matches the
// pattern in LabSettingsControllerIT and ResultCrPdfAcceptanceIT.
@SpringBootTest
@TestMethodOrder(MethodOrderer.MethodName.class)
class JdbcLabSettingsApiIT extends IntegrationTestBase {

    @Autowired private LabSettingsApi api;

    @AfterEach
    void restoreSeed() {
        api.update(
                new LabSettingsUpdateRequest(
                        "Laboratoire",
                        "Adresse à configurer",
                        "Numéro d'accréditation à configurer",
                        "Directeur à configurer"));
    }

    @Test
    void get_returnsSeededRow() {
        var s = api.get();

        assertThat(s.name()).isEqualTo("Laboratoire");
        assertThat(s.address()).isEqualTo("Adresse à configurer");
        assertThat(s.accreditationNumber()).isEqualTo("Numéro d'accréditation à configurer");
        assertThat(s.managerName()).isEqualTo("Directeur à configurer");
        assertThat(s.updatedAt()).isNotNull();
    }

    @Test
    void update_replacesAllFieldsAndBumpsUpdatedAt() {
        var before = api.get();
        var req =
                new LabSettingsUpdateRequest(
                        "Laboratoire Sparkcorp",
                        "12 Avenue du Test, Lomé",
                        "ACC-2026-001",
                        "Dr. KOFFI Adjoa");

        var after = api.update(req);

        assertThat(after.name()).isEqualTo("Laboratoire Sparkcorp");
        assertThat(after.address()).isEqualTo("12 Avenue du Test, Lomé");
        assertThat(after.accreditationNumber()).isEqualTo("ACC-2026-001");
        assertThat(after.managerName()).isEqualTo("Dr. KOFFI Adjoa");
        assertThat(after.updatedAt()).isAfterOrEqualTo(before.updatedAt());

        // Persistence: a fresh read returns the same.
        var readBack = api.get();
        assertThat(readBack.name()).isEqualTo("Laboratoire Sparkcorp");
    }
}
