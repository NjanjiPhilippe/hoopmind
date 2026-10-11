package com.np3tech.hoopmind;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Vérifie les frontières entre modules (pas de cycle, pas d'accès aux types internes d'un module fermé)
 * et génère la documentation des modules dans target/spring-modulith-docs.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(HoopMindApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        modules.verify();
    }

    /**
     * Les modules {@code ml} et {@code nba} restent ouverts à cause des relations JPA historiques
     * (MatchPrediction → Game…). Le nouveau moteur de prédiction, lui, ne lit le module NBA qu'à travers
     * son API publique ({@code GameHistory}, {@code GameRecord}) : cette règle empêche d'y revenir.
     */
    @Test
    void predictionEngineReadsNbaOnlyThroughItsPublicApi() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.np3tech.hoopmind");
        noClasses()
                .that().haveSimpleNameStartingWith("GameOutcome")
                .or().haveSimpleNameStartingWith("SparkGameOutcome")
                .or().haveSimpleName("TeamGameLoader")
                .or().haveSimpleName("GameFeatureBuilder")
                .or().haveSimpleName("TrainingReport")
                .or().resideInAPackage("com.np3tech.hoopmind.ml.web..")
                .or().resideInAPackage("com.np3tech.hoopmind.ml.scoring..")
                .or().resideInAPackage("com.np3tech.hoopmind.ml.training..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.np3tech.hoopmind.nba.entity..",
                        "com.np3tech.hoopmind.nba.repository..",
                        "com.np3tech.hoopmind.nba.service..",
                        "com.np3tech.hoopmind.nba.history..")
                .check(classes);
    }

    @Test
    void writeModuleDocumentation() {
        new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }
}
