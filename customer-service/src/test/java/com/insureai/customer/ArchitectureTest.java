package com.insureai.customer;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.insureai.customer", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest
    static final ArchRule domainDoesNotDependOnFrameworksOrOuterLayers = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "..application..",
            "..adapter..",
            "org.springframework..",
            "jakarta..",
            "com.fasterxml..",
            "tools.jackson.."
        );

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule inboundAdaptersDoNotDependOnOutboundAdapters = noClasses()
        .that().resideInAPackage("..adapter.inbound..")
        .should().dependOnClassesThat().resideInAPackage("..adapter.outbound..");

    @ArchTest
    static final ArchRule outboundAdaptersDoNotDependOnInboundAdapters = noClasses()
        .that().resideInAPackage("..adapter.outbound..")
        .should().dependOnClassesThat().resideInAPackage("..adapter.inbound..");

    @ArchTest
    static final ArchRule useCasesAndPortsAreInterfaces = classes()
        .that().resideInAnyPackage("..application.port..")
        .and().haveSimpleNameEndingWith("UseCase")
        .or().haveSimpleNameEndingWith("Port")
        .should().beInterfaces();
}
