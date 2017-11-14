# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
 - ?
## Changed
 - Update kotlin to 1.1.60 version
 - Bumped android tools versions to 3.0 and now support multidimension projects >0.10.x (#29)
### Fixed
 - ?

## [2.3.0] - 2017-11-13
### Added
 - Add configuration parameter `android` with default value to false
### Changed
 - Define a different output file for each sourceSet
### Fixed
- Fixed plugin doesn't apply custom reporter for ktlint versions >0.10.x (#28)

## [2.2.1] - 2017-10-06
### Fixed
 - Fixed report output is always opened since task is created

## [2.2.0] - 2017-10-05
### Added
 - Add configuration parameter `ignoreFailures` with default value to false
### Changed
 - Update default ktlint version to 0.9.2
### Fixed
 - Fixed report output is not closed after task run is finished (#25)

## [2.1.1] - 2017-08-15
### Changed
 - Update default ktlint version to 0.8.1
 - Fix extension version has no effect on used ktlint version
 - Add check task also depends on ktlintCheck task
 - Add output report
 - Add report type to extension

## [2.1.0] - 2017-07-5
### Added
 - Android support
 - Ability to only apply plugin to the root project that contains subprojects with kotlin code

## [2.0.1] - 2017-06-1
### Changed
 - Remove dependency on Gradle Script Kotlin plugin API. (#9)
 - Compatibility with Gradle v4.0. (#9)

## [2.0.0] - 2017-05-26
### Changed
 - Renamed task with name `ktlint` to `ktlintCheck` (#3)
 - Renamed tasks with names `ktlint[source set name]` to `ktlint[source set name]Check` (#3)
