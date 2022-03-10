# Releasing

How to perform a release of the plugin.

1. Update the `CHANGELOG.md` and `VERSION.txt` file by running the
   [Prepare for Release](https://github.com/JLLeitschuh/ktlint-gradle/actions/workflows/prepare-release.yaml)
   GitHub action.
2. Verify that the [CHANGELOG.md](https://github.com/JLLeitschuh/ktlint-gradle/blob/master/CHANGELOG.md) file looks good.

   **NOTE**: The `## [Unreleased]` header should still remain in the document, but should be empty.
   This is expected by the GitHub release note upload step.
3. Run the
   [Perform Release](https://github.com/JLLeitschuh/ktlint-gradle/actions/workflows/perform-release.yaml) GitHub action.
   This will actually perform the release.
