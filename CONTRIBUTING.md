# Contributing

## Common Tasks

### Updating Gradle

1. Find the latest version and latest checksums here: https://gradle.org/release-checksums/
2. Update the `gradleWrapper` and `gradleWrapperSha` version in the [version file](plugin/gradle/libs.versions.toml).
3. In this directory run the following command twice:
   ```bash
   ./gradlew wrapper
   ```
    This will update the `gradle/gradle-wrapper.properties` and `gradle/gradle-wrapper.jar` files.
4. Now run the following command twice:
   ```bash
   ./plugin/gradlew wrapper
   ```
   This will update the `plugin/gradle/gradle-wrapper.properties` and `plugin/gradle/gradle-wrapper.jar` files.

The reason this is required is that the `plugin` itself is an included build of the parent project which is actually a gradle build intended to execute and test the `samples`.

### Releasing

Note: This section is only relevant for maintainers of the project.

How to perform a release of the plugin.

1. Trigger a new release with the
   [Trigger Release](https://github.com/JLLeitschuh/ktlint-gradle/actions/workflows/prepare-release.yaml)
   Workflow. For non-rc tagged versions, this will update the `CHANGELOG.md` file automatically.

2. Verify that the
   [CHANGELOG.md](https://github.com/JLLeitschuh/ktlint-gradle/blob/main/CHANGELOG.md)
   file looks good.

   **NOTE**: If publishing a release candidate (rc) version, the `CHANGELOG.md` file will not be updated automatically.
   You will need to inspect the `CHANGELOG.md` and the `README.md` files that are uploaded as workflow artifacts.

   **NOTE**: The `## [Unreleased]` header should still remain, but should now be empty.
   This is expected by the GitHub release note upload step.

3. Await the completion of the
   [New Release](https://github.com/JLLeitschuh/ktlint-gradle/actions/workflows/prepare-release.yaml)
   Workflow. This will create a new release on GitHub and upload the release notes to the release.
