package org.sparkcorporation.lims.pdf.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import org.sparkcorporation.lims.LimsApplication;
import org.sparkcorporation.lims.pdf.api.PdfDocumentType;

class PdfModuleStructureTest {

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
    void pdfApiIsNotAllowedToImportPdfInfra() {
        noClasses()
                .that()
                .resideInAPackage("..pdf.api..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..pdf.infra..")
                .check(production);
    }

    @Test
    void pdfInfraDoesNotImportFromAnyConsumerModule() {
        noClasses()
                .that()
                .resideInAPackage("..pdf.infra..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..result..",
                        "..billing..",
                        "..cashbox..",
                        "..order..",
                        "..quote..",
                        "..iam.domain..",
                        "..catalog..",
                        "..patient..")
                .check(production);
    }

    @Test
    void everyPdfDocumentTypeImplementerLivesUnderConsumerInfraPdf() {
        classes()
                .that()
                .implement(PdfDocumentType.class)
                .should()
                .resideInAPackage("..infra.pdf..")
                .check(production);
    }

    @Test
    void noWebControllerImportsThymeleafApi() {
        noClasses()
                .that()
                .resideInAnyPackage("..web..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("org.thymeleaf..")
                .check(production);
    }
}
