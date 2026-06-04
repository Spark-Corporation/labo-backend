package org.sparkcorporation.lims.attachment.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import org.sparkcorporation.lims.LimsApplication;

class AttachmentModuleStructureTest {

    private static JavaClasses production;

    @BeforeAll
    static void setUp() {
        production =
                new ClassFileImporter()
                        .withImportOption(new ImportOption.DoNotIncludeTests())
                        .importPackages("org.sparkcorporation.lims");
    }

    @Test
    void springModulithModuleBoundariesAreValid() {
        ApplicationModules.of(LimsApplication.class).verify();
    }

    @Test
    void attachmentApiDoesNotImportInfraOrDomain() {
        noClasses()
                .that()
                .resideInAPackage("..attachment.api..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..attachment.infra..", "..attachment.domain..")
                .check(production);
    }

    @Test
    void attachmentModuleDependsOnNoOtherLimsModule() {
        noClasses()
                .that()
                .resideInAPackage("..lims.attachment..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..lims.order..",
                        "..lims.cashbox..",
                        "..lims.result..",
                        "..lims.patient..",
                        "..lims.iam..",
                        "..lims.billing..")
                .check(production);
    }
}
