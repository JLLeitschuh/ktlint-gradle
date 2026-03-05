import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.util.prefixIfNot

plugins {
    id("com.gradle.plugin-publish")
    `kotlin-dsl`
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint")
    id("com.gradleup.shadow")
    id("com.github.breadmoirai.github-release")
    id("com.netflix.nebula.release")
}

val pluginGroup = "org.jlleitschuh.gradle"
group = pluginGroup
description = "This plugin creates convenient tasks in your Gradle project that run ktlint checks or do code auto format."
repositories {
    google()
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

ktlint {
    version.set("1.6.0")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_8)
        apiVersion.set(KotlinVersion.KOTLIN_1_8)
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

dependencies {
    compileOnly("com.pinterest.ktlint:ktlint-cli-reporter-core:1.8.0")
    compileOnly("com.pinterest.ktlint:ktlint-rule-engine:1.8.0")
    compileOnly("com.pinterest.ktlint:ktlint-ruleset-standard:1.8.0")
    compileOnly("com.pinterest.ktlint:ktlint-cli-reporter-baseline:1.8.0")
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.android.gradle.plugin)
    compileOnly(kotlin("stdlib-jdk8"))
    implementation(libs.semver)
    implementation(libs.jgit)
    implementation(libs.commons.io)

    testImplementation(libs.assertj.core)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.ktlint.rule.engine)
    testImplementation(libs.archunit.junit5)
}

fun JvmTestSuite.extendFromTest() {
    sources {
        java {
            source(project.sourceSets.named("test").map { it.java }.get())
        }
        kotlin {
            source(project.sourceSets.named("test").map { it.kotlin }.get())
        }
        resources {
            source(project.sourceSets.named("test").map { it.resources }.get())
        }
        compileClasspath += project.sourceSets["test"].compileClasspath + project.sourceSets["main"].compileClasspath
        runtimeClasspath += project.sourceSets["test"].runtimeClasspath + project.sourceSets["main"].runtimeClasspath

        // make internal classes available to tests
        val compilations = project
            .extensions
            .getByType(KotlinJvmProjectExtension::class.java).target.compilations
        compilations.getByName(sources.name)
            .associateWith(compilations.getByName(SourceSet.MAIN_SOURCE_SET_NAME))
    }
}

fun getCurrentJavaVersion(): String {
    return Runtime::class.java.getPackage().specificationVersion // java 8
        ?: Runtime::class.java.getMethod("version").invoke(null).toString() // java 9+
}
testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter()
        }
        listOf(8, 11, 17, 21, 25)
            .forEach {
                register<JvmTestSuite>("test$it") {
                    extendFromTest()
                    targets.all {
                        dependencies {
                            implementation(gradleTestKit())
                        }
                        testTask.configure {
                            javaLauncher.set(
                                javaToolchains.launcherFor {
                                    languageVersion.set(JavaLanguageVersion.of(JavaVersion.current().majorVersion))
                                }
                            )
                        }
                    }
                }
            }
    }
}
tasks.withType<Test> {
    dependsOn("publishToMavenLocal")
    maxParallelForks = 6 // no point in this being higher than the number of gradle workers

    // Set the system property for the project version to be used in the tests
    systemProperty("project.version", project.version.toString())
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    develocity.testRetry {
        val isCiServer = System.getenv().containsKey("CI")
        if (isCiServer) {
            maxRetries.set(2)
            maxFailures.set(10)
        }
    }
}

val shadowJarTask = tasks.named<ShadowJar>("shadowJar") {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to project.group,
            "Implementation-Vendor-Id" to project.group
        )
    }
    relocate("com.googlecode.javaewah", "$pluginGroup.shadow.com.googlecode.javaewah")
    relocate("net.swiftzer", "$pluginGroup.shadow.net.swiftzer")
    relocate("org.apache.commons.io", "$pluginGroup.shadow.org.apache.commons.io")
    relocate("org.eclipse.jgit", "$pluginGroup.shadow.org.eclipse.jgit")
    relocate("org.slf4j", "$pluginGroup.shadow.org.slf4j")
    archiveClassifier.set(null)
}

// Add shadow jar to the Gradle module metadata api and runtime configurations
artifacts {
    runtimeElements(shadowJarTask)
    apiElements(shadowJarTask)
}

tasks.whenTaskAdded {
    if (name == "publishPluginJar" || name == "generateMetadataFileForPluginMavenPublication") {
        dependsOn(tasks.named("shadowJar"))
    }
}

tasks.named("check") {
    dependsOn(tasks.named("assemble"))
}

/**
 * Configures the publishing environment for publishing with Travis CI.
 * All you need to do is push a tagged commit to github and Travis CI will automatically publish a
 * release of the plugin using the current [Project.getVersion].
 */
fun setupPublishingEnvironment() {
    val keyEnvironmentVariable = "GRADLE_PUBLISH_KEY"
    val secretEnvironmentVariable = "GRADLE_PUBLISH_SECRET"
    val githubEnvironmentVariable = "GITHUB_KEY"

    val keyProperty = "gradle.publish.key"
    val secretProperty = "gradle.publish.secret"
    val githubProperty = "github.secret"

    if (System.getProperty(keyProperty) == null || System.getProperty(secretProperty) == null) {
        logger
            .info(
                "`$keyProperty` or `$secretProperty` were not set. Attempting to configure from environment variables"
            )

        val key: String? = System.getenv(keyEnvironmentVariable)
        val secret: String? = System.getenv(secretEnvironmentVariable)
        if (key != null && secret != null) {
            System.setProperty(keyProperty, key)
            System.setProperty(secretProperty, secret)
        } else {
            logger.warn("Publishing key or secret was null")
        }
    }

    if (System.getProperty(githubProperty) == null) {
        logger.info(
            "`$githubProperty` was not set. Attempting to configure it from environment variable"
        )

        val key: String? = System.getenv(githubEnvironmentVariable)
        if (key != null) {
            System.setProperty(githubProperty, key)
        } else {
            logger.warn("GitHub key was null")
        }
    }
}

setupPublishingEnvironment()

configurations.named("runtimeClasspath") {
    attributes {
        attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named("7.4"))
    }
}

gradlePlugin {
    vcsUrl = "https://github.com/JLLeitschuh/ktlint-gradle"
    website = vcsUrl
    (plugins) {
        register("ktlintPlugin") {
            id = "org.jlleitschuh.gradle.ktlint"
            implementationClass = "org.jlleitschuh.gradle.ktlint.KtlintPlugin"
            displayName = "Ktlint Gradle Plugin"
            tags = listOf("ktlint", "kotlin", "linting")
            description = project.description
        }
    }
}

tasks.named<ValidatePlugins>("validatePlugins").configure {
    enableStricterValidation = true
}

githubRelease {
    setToken(providers.systemProperty("github.secret"))
    owner = "JLLeitschuh"
    repo = "ktlint-gradle"
    overwrite = true
    releaseAssets(tasks.named("shadowJar"))

    fun isPreReleaseVersion(): Boolean {
        val version = project.version.toString()
        return version.contains("-rc") ||
            version.contains("-dev") ||
            version.contains("-SNAPSHOT")
    }

    prerelease.set(provider { isPreReleaseVersion() })
    body.set(
        provider {
            // If publishing a rc version, use the [Unreleased] section of the changelog
            if (isPreReleaseVersion()) {
                projectDir.resolve("../CHANGELOG.md")
                    .readText()
                    .substringAfter("## [")
                    .substringBefore("## [")
                    .prefixIfNot("## [")
                    .replace("## [Unreleased]", "## [${project.version}]")
            } else {
                projectDir.resolve("../CHANGELOG.md")
                    .readText()
                    .substringAfter("## [")
                    .substringAfter("## [")
                    .substringBefore("## [")
                    .prefixIfNot("## [")
            }
        }
    )
    dryRun.set(providers.systemProperty("dryRun").orElse("false").map { it.toBoolean() })
}

tasks.named<GithubReleaseTask>("githubRelease") {
    doLast {
        if (tagName.get().startsWith("v0.1.0")) {
            throw GradleException(
                "Release version (${tagName.get()}) was not correctly detected. " +
                    "Please check that the git repository is correctly initialized and the tag is correct. " +
                    "For GiHub Actions environments, check the fetch-depth setting in the actions/checkout step."
            )
        }
    }
}

tasks.withType<Wrapper>().configureEach {
    gradleVersion = libs.versions.gradleWrapper.get()
    distributionSha256Sum = libs.versions.gradleWrapperSha.get()
    distributionType = Wrapper.DistributionType.BIN
}
