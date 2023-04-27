package org.jlleitschuh.gradle.ktlint

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

@AnalyzeClasses(
    packages = ["org.jlleitschuh.gradle.ktlint.."]
)
internal class KtLintClassesUsageScopeTest {
    @ArchTest
    val `Non-worker plugin classes should not use ktlint classes` = noClasses()
        .that()
        .resideInAnyPackage(
            "org.jlleitschuh.gradle.ktlint",
            "org.jlleitschuh.gradle.ktlint.android",
            "org.jlleitschuh.gradle.ktlint.tasks"
        )
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.pinterest.ktlint..")
}
