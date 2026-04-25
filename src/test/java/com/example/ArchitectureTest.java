package com.example;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests that enforce hexagonal architecture rules at build time.
 * Uses noClasses() API to avoid ArchUnit version-specific layeredArchitecture() behaviour.
 */
@AnalyzeClasses(
        packages = "com.example",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    // ── 1. Domain must not depend on any other project layer ─────────────────

    @ArchTest
    static final ArchRule domainMustNotDependOnApplicationLayer =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application..")
                    .because("Domain must not depend on the application layer — " +
                             "it is the innermost layer with no inward dependencies.");

    @ArchTest
    static final ArchRule domainMustNotDependOnAdapters =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapters..")
                    .because("Domain must not depend on adapters.");

    @ArchTest
    static final ArchRule domainMustNotDependOnSpringOrPersistence =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.springframework.data..")
                    .because("Domain layer must be framework-agnostic. " +
                             "Framework annotations belong in the adapters layer.");

    @ArchTest
    static final ArchRule domainMustNotHaveMongoDocumentAnnotation =
            noClasses().that().resideInAPackage("..domain..")
                    .should().beAnnotatedWith(
                            org.springframework.data.mongodb.core.mapping.Document.class)
                    .because("@Document is a MongoDB infrastructure annotation and must " +
                             "never appear in the domain layer.");

    // ── 2. Application must not depend on adapters ────────────────────────────

    @ArchTest
    static final ArchRule applicationMustNotDependOnAdapters =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapters..")
                    .because("Application layer may only depend inward on the domain. " +
                             "Adapters must depend on application ports, not the other way around.");

    // ── 3. Inbound adapters must not depend on outbound adapters ─────────────

    @ArchTest
    static final ArchRule inboundAdaptersMustNotDependOnOutboundAdapters =
            noClasses().that().resideInAPackage("..adapters.in..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapters.out..")
                    .because("Inbound adapters (REST) must not know about outbound adapters (MongoDB). " +
                             "Cross-adapter communication must go through application ports.");

    // ── 4. REST controllers must not use the persistence port directly ────────

    @ArchTest
    static final ArchRule restAdaptersMustNotImportPersistencePort =
            noClasses().that().resideInAPackage("..adapters.in.rest..")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName(
                            "com.example.user.application.port.out.UserPersistencePort")
                    .because("REST controllers must only talk to inbound ports (use cases), " +
                             "never directly to the persistence port.");

    // ── 5. Controllers must be annotated @RestController ─────────────────────

    @ArchTest
    static final ArchRule controllersMustBeAnnotatedWithRestController =
            classes().that().resideInAPackage("..adapters.in.rest")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().beAnnotatedWith(
                            org.springframework.web.bind.annotation.RestController.class)
                    .because("Every class named *Controller in the REST adapter must be " +
                             "annotated with @RestController.");

    // ── 6. Outbound port implementations must reside in adapters.out ─────────

    @ArchTest
    static final ArchRule persistencePortImplementationsMustBeInAdaptersOut =
            classes().that().implement(
                            com.example.user.application.port.out.UserPersistencePort.class)
                    .should().resideInAPackage("..adapters.out..")
                    .because("Implementations of outbound ports are infrastructure concerns " +
                             "and must live in the adapters.out layer.");
}
