import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.util.prefixIfNot

plugins {
    kotlin("jvm")
    id("com.gradle.plugin-publish")
    `java-gradle-plugin`
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint")
    id("com.github.johnrengelman.shadow")
    id("com.github.breadmoirai.github-release")
    id("org.gradle.test-retry")
}

val pluginGroup = "org.jlleitschuh.gradle"
group = pluginGroup
version = projectDir.resolve("VERSION_CURRENT.txt").readText().trim()

repositories {
    google()
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        apiVersion = "1.3"
        jvmTarget = "1.8"
    }
}

tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.from(configurations.compileOnly)
}

/**
 * Special configuration to be included in resulting shadowed jar, but not added to the generated pom and gradle
 * metadata files.
 */
val shadowImplementation by configurations.creating
configurations {
    val compileOnly by getting {
        extendsFrom(shadowImplementation)
        isCanBeResolved = true
    }
}
configurations["compileOnly"].extendsFrom(shadowImplementation)
configurations["testImplementation"].extendsFrom(shadowImplementation)
sourceSets {
    val adapter by creating {
    }
    val adapter34 by creating {
        compileClasspath += adapter.output
    }
    val adapter41 by creating {
        compileClasspath += adapter.output
    }
    val adapter45 by creating {
        compileClasspath += adapter.output
    }
    val adapter46 by creating {
        compileClasspath += adapter.output
    }
    val adapter47 by creating {
        compileClasspath += adapter.output
    }
    val adapter48 by creating {
        compileClasspath += adapter.output
    }
    val adapter49 by creating {
        compileClasspath += adapter.output
    }
    val adapters = listOf(adapter, adapter34, adapter41, adapter45, adapter46, adapter47, adapter48, adapter49)
    val main by getting {
        kotlin {
            compileClasspath = adapters.map { it.output }.fold(compileClasspath) { a, b -> a + b }
            runtimeClasspath = adapters.map { it.output }.fold(runtimeClasspath) { a, b -> a + b }
        }
    }
    val test by getting {
        kotlin {
            compileClasspath = adapters.map { it.output }.fold(compileClasspath) { a, b -> a + b }
            runtimeClasspath = adapters.map { it.output }.fold(runtimeClasspath) { a, b -> a + b }
        }
    }
}
val adapterSources = listOf(
    sourceSets.named("adapter"),
    sourceSets.named("adapter34"),
    sourceSets.named("adapter41"),
    sourceSets.named("adapter45"),
    sourceSets.named("adapter46"),
    sourceSets.named("adapter47"),
    sourceSets.named("adapter48"),
    sourceSets.named("adapter49")
)
tasks.named<Jar>("shadowJar") {
    this.from(adapterSources.map { sourceSet -> sourceSet.map { it.output.classesDirs } })
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.ktlint.core)
    add("adapterCompileOnly", libs.ktlint.core)
    add("adapterImplementation", libs.semver)
    add("adapter34Implementation", kotlin("reflect"))
    add("adapter34CompileOnly", "com.pinterest.ktlint:ktlint-core:0.34.0")
    add("adapter41CompileOnly", "com.pinterest.ktlint:ktlint-core:0.41.0")
    add("adapter45CompileOnly", "com.pinterest.ktlint:ktlint-core:0.45.2")
    add("adapter46CompileOnly", "com.pinterest.ktlint:ktlint-core:0.46.1")
    add("adapter47CompileOnly", "com.pinterest.ktlint:ktlint-core:0.47.1")
    add("adapter48CompileOnly", "com.pinterest.ktlint:ktlint-core:0.48.2")
  //  add("adapter49CompileOnly", "com.pinterest.ktlint:ktlint-rule-engine-core:0.49.0")
    add("adapter49CompileOnly", "com.pinterest.ktlint:ktlint-core:0.49.1")
    add("adapter49CompileOnly", "com.pinterest.ktlint:ktlint-cli-reporter:0.49.1")
    add("adapter49CompileOnly", "com.pinterest.ktlint:ktlint-rule-engine:0.49.1")
    add("adapter49CompileOnly", "com.pinterest.ktlint:ktlint-ruleset-standard:0.49.1")
    add("adapter49CompileOnly","com.pinterest.ktlint:ktlint-reporter-baseline:0.49.1")
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.android.gradle.plugin)
    compileOnly(kotlin("stdlib-jdk8"))
    shadowImplementation(libs.semver)
    shadowImplementation(libs.jgit)
    shadowImplementation(libs.commons.io)
    // Explicitly added for shadow plugin to relocate implementation as well
    shadowImplementation(libs.slf4j.nop)

    /*
     * Do not depend upon the gradle script kotlin plugin API. IE: gradleScriptKotlinApi()
     * It's currently in flux and has binary breaking changes in gradle 4.0
     * https://github.com/JLLeitschuh/ktlint-gradle/issues/9
     */

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.ktlint.core)
    testImplementation(libs.archunit.junit5)
}

// Test tasks loods plugin from local maven repository
tasks.named("test").configure {
    dependsOn("publishToMavenLocal")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    doFirst {
        logger.lifecycle("maxParallelForks for '$path' is $maxParallelForks")
    }
    testLogging {
        events(
            TestLogEvent.STARTED,
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    retry {
        val isCiServer = System.getenv().containsKey("CI")
        if (isCiServer) {
            maxRetries.set(2)
            maxFailures.set(10)
        }
    }
}

val relocateShadowJar = tasks.register<ConfigureShadowRelocation>("relocateShadowJar")
val shadowJarTask = tasks.named<ShadowJar>("shadowJar") {
    // Enable package relocation in resulting shadow jar
    relocateShadowJar.get().apply {
        prefix = "$pluginGroup.shadow"
        target = this@named
    }

    dependsOn(relocateShadowJar)
    minimize()
    archiveClassifier.set("")
    configurations = listOf(shadowImplementation)
}

// Add shadow jar to the Gradle module metadata api and runtime configurations
configurations {
    artifacts {
        runtimeElements(shadowJarTask)
        apiElements(shadowJarTask)
    }
}

tasks.whenTaskAdded {
    if (name == "publishPluginJar" || name == "generateMetadataFileForPluginMavenPublication") {
        dependsOn(tasks.named("shadowJar"))
    }
}

// Disabling default jar task as it is overridden by shadowJar
tasks.named("jar").configure {
    enabled = false
}

val ensureDependenciesAreInlined by tasks.registering {
    description = "Ensures all declared dependencies are inlined into shadowed jar"
    group = HelpTasksPlugin.HELP_GROUP
    dependsOn(tasks.shadowJar)

    doLast {
        val nonInlinedDependencies = mutableListOf<String>()
        zipTree(tasks.shadowJar.flatMap { it.archiveFile }).visit {
            if (!isDirectory) {
                val path = relativePath
                if (!path.startsWith("META-INF") &&
                    path.lastName.endsWith(".class") &&
                    !path.pathString.startsWith(pluginGroup.replace(".", "/"))
                ) {
                    nonInlinedDependencies.add(path.pathString)
                }
            }
        }
        if (nonInlinedDependencies.isNotEmpty()) {
            throw GradleException("Found non inlined dependencies: $nonInlinedDependencies")
        }
    }
}
tasks.named("check") {
    dependsOn(ensureDependenciesAreInlined)
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
            .info("`$keyProperty` or `$secretProperty` were not set. Attempting to configure from environment variables")

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

gradlePlugin {
    (plugins) {
        register("ktlintPlugin") {
            id = "org.jlleitschuh.gradle.ktlint"
            implementationClass = "org.jlleitschuh.gradle.ktlint.KtlintPlugin"
        }
        register("ktlintIdeaPlugin") {
            id = "org.jlleitschuh.gradle.ktlint-idea"
            implementationClass = "org.jlleitschuh.gradle.ktlint.KtlintIdeaPlugin"
        }
    }
}

// Need to move publishing configuration into afterEvaluate {}
// to override changes done by "com.gradle.plugin-publish" plugin in afterEvaluate {} block
// See PublishPlugin class for details
afterEvaluate {
    publishing {
        publications {
            withType<MavenPublication> {
                // Special workaround to publish shadow jar instead of normal one. Name to override peeked here:
                // https://github.com/gradle/gradle/blob/master/subprojects/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/MavenPluginPublishPlugin.java#L73
                if (name == "pluginMaven") {
                    setArtifacts(
                        listOf(
                            shadowJarTask.get()
                        )
                    )
                }
            }
        }
    }
}

pluginBundle {
    vcsUrl = "https://github.com/JLLeitschuh/ktlint-gradle"
    website = vcsUrl
    description = "Provides a convenient wrapper plugin over the ktlint project."
    tags = listOf("ktlint", "kotlin", "linting")

    (plugins) {
        "ktlintPlugin" {
            displayName = "Ktlint Gradle Plugin"
        }
        "ktlintIdeaPlugin" {
            displayName = "Ktlint Gradle IntelliJ Configuration Plugin"
        }
    }
}

githubRelease {
    setToken(System.getProperty("github.secret"))
    setOwner("JLLeitschuh")
    setRepo("ktlint-gradle")
    setOverwrite(true)
    body {
        projectDir.resolve("../CHANGELOG.md")
            .readText()
            .substringAfter("## [")
            .substringAfter("## [")
            .substringBefore("## [")
            .prefixIfNot("## [")
    }
}

tasks.withType<Wrapper>().configureEach {
    gradleVersion = libs.versions.gradleWrapper.get()
    distributionSha256Sum = libs.versions.gradleWrapperSha.get()
    distributionType = Wrapper.DistributionType.BIN
}
