package org.sparkcorporation.lims.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Guards the {@code shared} kernel against accidental inbound dependencies on any other LIMS
 * package.
 *
 * <p>Non-brittle: does not enumerate downstream module names. Adding a new module later (e.g.
 * {@code billing}, {@code cashbox}) does not require updating this test.
 *
 * <p>This complements {@code ModuleStructureTest}'s {@code ApplicationModules.verify()} run.
 */
@AnalyzeClasses(packages = "org.sparkcorporation.lims")
class SharedKernelArchitectureTest {

    @ArchTest
    static final ArchRule shared_has_no_inbound_lims_dependencies =
            noClasses()
                    .that()
                    .resideInAPackage("..shared..")
                    .should()
                    .dependOnClassesThat(
                            resideInAPackage("org.sparkcorporation.lims..")
                                    .and(not(resideInAPackage("..shared.."))));
}
