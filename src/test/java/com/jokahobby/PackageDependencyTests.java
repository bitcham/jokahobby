package com.jokahobby;


import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

public class PackageDependencyTests {

    private static final String HOBBY = "..modules.hobby..";
    private static final String EVENT = "..modules.event..";
    private static final String ACCOUNT = "..modules.account..";
    private static final String TAG = "..modules.tag..";
    private static final String ZONE = "..modules.zone..";

    @Test
    public void some_architecture_rule() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("com.jokahobby");

        ArchRule hobbyPackageRule = classes().that().resideInAnyPackage("..modules.hobby..")
                .should().onlyBeAccessed().byClassesThat()
                .resideInAnyPackage(HOBBY, EVENT);

        ArchRule eventPackageRule = classes().that().resideInAPackage(EVENT)
                .should().accessClassesThat().resideInAnyPackage(HOBBY, ACCOUNT, EVENT);

        ArchRule accountPackageRule = classes().that().resideInAPackage(ACCOUNT)
                .should().accessClassesThat().resideInAnyPackage(TAG, ZONE, ACCOUNT);


        ArchRule cycleCheck = slices().matching("com.jokahobby.modules.(*)..")
                .should().beFreeOfCycles();

        hobbyPackageRule.check(importedClasses);
        eventPackageRule.check(importedClasses);
        accountPackageRule.check(importedClasses);
        cycleCheck.check(importedClasses);

    }





}
