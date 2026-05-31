package org.sparkcorporation.lims.export.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import org.sparkcorporation.lims.LimsApplication;
import org.sparkcorporation.lims.export.api.OfficeDocumentType;

class ExportModuleStructureTest {

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
    void exportApiIsNotAllowedToImportExportInfra() {
        noClasses()
                .that()
                .resideInAPackage("..export.api..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..export.infra..")
                .check(production);
    }

    @Test
    void everyOfficeDocumentTypeImplementerLivesUnderConsumerInfraExport() {
        classes()
                .that()
                .implement(OfficeDocumentType.class)
                .should()
                .resideInAPackage("..infra.export..")
                .allowEmptyShould(true)
                .check(production);
    }
}
