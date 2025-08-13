import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gradle.enterprise.gradleplugin.testretry.retry
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.util.prefixIfNot

plugins {
    id("com.gradle.plugin-publish")
    `kotlin-dsl`
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint")
    id("com.github.johnrengelman.shadow")
    id("com.github.breadmoirai.github-release")
    id("com.netflix.nebula.release")
}

val pluginGroup = "org.jlleitschuh.gradle"
group = pluginGroup

repositories {
    google()
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

ktlint {
    version.set("1.6.0")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_7)
        apiVersion.set(KotlinVersion.KOTLIN_1_7)
        jvmTarget.set(JvmTarget.JVM_1_8)
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

    val adapter100 by creating {
        compileClasspath += adapter.output
    }
    val adapters = listOf(
        adapter,
        adapter100
    )
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
    sourceSets.named("adapter100")
)

tasks.named<Jar>("shadowJar").configure {
    this.from(adapterSources.map { sourceSet -> sourceSet.map { it.output.classesDirs } })
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to project.group,
            "Implementation-Vendor-Id" to project.group
        )
    }
}

dependencies {
    add("adapterImplementation", libs.commons.io)
    add("adapterImplementation", libs.semver)

    add("adapter100CompileOnly", "com.pinterest.ktlint:ktlint-cli-reporter-core:1.0.0")
    add("adapter100CompileOnly", "com.pinterest.ktlint:ktlint-rule-engine:1.0.0")
    add("adapter100CompileOnly", "com.pinterest.ktlint:ktlint-ruleset-standard:1.0.0")
    add("adapter100CompileOnly", "com.pinterest.ktlint:ktlint-cli-reporter-baseline:1.0.0")

    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.android.gradle.plugin)
    compileOnly(kotlin("stdlib-jdk8"))
    shadowImplementation(libs.semver)
    shadowImplementation(libs.jgit)
    shadowImplementation(libs.commons.io)
    // Explicitly added for shadow plugin to relocate implementation as well
    shadowImplementation(libs.slf4j.nop)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.ktlint.rule.engine)
    testImplementation(libs.archunit.junit5)
    testImplementation(gradleApi())
    // Used to test the problems API
    testImplementation(libs.mockito.kotlin)
}

kotlin {
    // set up friend paths so that we can use internal classes across source sets
    target.compilations.forEach {
        if (it.name.startsWith("adapter")) {
            if (it.name != "adapter") {
                it.associateWith(target.compilations.getByName("adapter"))
            }
            target.compilations.getByName("main").associateWith(it)
        }
    }
}

tasks.withType<Test> {
    dependsOn("publishToMavenLocal")
    useJUnitPlatform()
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

    retry {
        val isCiServer = System.getenv().containsKey("CI")
        if (isCiServer) {
            maxRetries.set(2)
            maxFailures.set(10)
        }
    }

    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.current().majorVersion))
        }
    )
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
    dependsOn(tasks.named("assemble"))
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

configurations.named("implementation") {
    attributes {
        attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named("7.4"))
    }
}

gradlePlugin {
    (plugins) {
        register("ktlintPlugin") {
            id = "org.jlleitschuh.gradle.ktlint"
            implementationClass = "org.jlleitschuh.gradle.ktlint.KtlintPlugin"
            displayName = "Ktlint Gradle Plugin"
        }
    }
}

tasks.named<ValidatePlugins>("validatePlugins").configure {
    enableStricterValidation = true
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
    }
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
