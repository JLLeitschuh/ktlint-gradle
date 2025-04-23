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
