package org.sparkcorporation.lims.labsettings.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import org.sparkcorporation.lims.LimsApplication;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;

class LabSettingsModuleStructureTest {

    private static JavaClasses production;

    @BeforeAll
    static void importClasses() {
        production =
                new ClassFileImporter()
                        .withImportOption(new ImportOption.DoNotIncludeTests())
                        .importPackages("org.sparkcorporation.lims");
    }

    @Test
    void springModulithVerifies() {
        ApplicationModules.of(LimsApplication.class).verify();
    }

    @Test
    void api_doesNotDependOn_infra() {
        noClasses()
                .that()
                .resideInAPackage("..labsettings.api..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..labsettings.infra..")
                .check(production);
    }

    @Test
    void infra_doesNotDependOn_otherModulesExceptShared() {
        noClasses()
                .that()
                .resideInAPackage("..labsettings.infra..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..result..",
                        "..pdf..",
                        "..billing..",
                        "..cashbox..",
                        "..order..",
                        "..patient..",
                        "..catalog..")
                .check(production);
    }

    @Test
    void exactlyOneImplementation_of_LabSettingsApi() {
        var impls =
                production.stream()
                        .filter(c -> c.isAssignableTo(LabSettingsApi.class))
                        .filter(c -> !c.isInterface())
                        .toList();
        assertThat(impls)
                .as("LabSettingsApi implementations")
                .extracting(c -> c.getSimpleName())
                .containsExactly("JdbcLabSettingsApi");
    }
}
