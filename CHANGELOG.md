<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Android-Package-Renamer Changelog

## [Unreleased]

### Added

- Support for the latest JetBrains IDEs up to build 262 (IntelliJ IDEA 2026.2, Android Studio 2026.1.3)
- Plugin Verifier checks against the latest IntelliJ IDEA and Android Studio releases
- New IDE-independent `core` module (`com.github.nguyenphuc22.androidpackagerenamer.core`) containing the
  package-renaming engine, reusable from any JVM tooling
- Command-line interface (`package-renamer-core`) to rename an Android project from the terminal,
  including `--dry-run` preview
- Automated integration tests for the core module that rename real Android project fixtures
  (manifest/gradle, Groovy/Kotlin DSL, data binding, build-directory cleanup)
- Dedicated CI job for the core module with an end-to-end CLI test

### Changed

- Migrated from the deprecated Gradle IntelliJ Plugin (1.x) to the IntelliJ Platform Gradle Plugin (2.18.1)
- Updated build toolchain: Gradle 9.7, Kotlin 2.3.21, Java 21 in CI, Gradle Changelog Plugin 2.5.0, Kover 0.9.9
- Removed the deprecated Gradle Qodana Plugin
- Refactored the IntelliJ plugin into a thin adapter that delegates file operations to the `core` module
- Removed duplicated package-name parsing from `WorkingPackage`/`ManagerFile` in favor of the shared core logic
- Code coverage gate enforced on the core module (minimum 85% line coverage) and reported to CodeCov

### Fixed

- Plugin not loading in IntelliJ IDEA 2025.2+ and Android Studio Narwhal 4+ due to the outdated `until-build` range
- CI build failure caused by the leftover plugin-template sample services (`MyProjectService`) whose tests relied on the `CI` environment variable and crashed when its mock `Project` name was `null`
- `applicationIdSuffix` being mistaken for `applicationId` when reading or rewriting `build.gradle(.kts)`
- Manifest `package` attribute being inserted into modern (AGP 8+) projects that do not declare it
- Renaming to a sub-package of the old package (`com.example` → `com.example.app`) corrupting the directory tree; now rejected with a clear error
- Identifiers that only share a prefix with the old package (e.g. `com.example.appx`) being rewritten by a naive string replace; replaced with word-boundary matching
- Partial renames when an explicit `--old-package` does not match the project; the old package is now validated before any file is touched
- Groovy `namespace "x"` (double-quoted) and namespace-only library modules not being handled

## [1.0.0] - 2025-07-05

### Added

- Comprehensive test suite with 11 test files covering core functionality
- PSI-based package refactoring with Android fallback support
- Improved error handling and validation

### Changed

- Enhanced code quality by removing deprecated functions
- Improved project structure and maintainability
- Updated to stable 1.0.0 release marking production readiness

### Fixed

- Resolved import issues and compilation errors
- Fixed IOException import in ManagerFile.kt

## [0.1.8] - 2024-06-20

- Update plugin to support latest JetBrains IDE versions.

## [0.1.7] - 2024-11-04

- Update version support plugin
- Fix bug write permission

## [0.1.6] - 2024-06-12

- Update version support plugin
- New Action in Tools Menu

## [0.1.5] - 2024-04-17

- Update version support plugin

## [0.1.4] - 2024-02-10

### Added

- Update version support plugin

## [0.1.3] - 2023-09-19

### Added

- Add namespace when it not exits.

## [0.1.2] - 2023-08-23

### Added

- Remove rename manifest.

## [0.1.1] - 2023-05-23

### Added

- Check null Folder

## [0.1.0] - 2023-02-7

### Added

- Added notification when package name is not found

## [0.0.9] - 2023-02-6

### Added

- Support Gradle KTS

## [0.0.8] - 2023-02-1

### Added

- Support DataBinding multiple layout xml
- Fix bug navigation component file xml
- Fix bug package and application id different
- Support Android Studio Arctic fox

## [0.0.7] - 2023-01-30

### Added

- Support DataBinding Library

## [0.0.6] - 2023-01-27

### Added

- Fix bug move file.

## [0.0.5-alpha] - 2023-01-25

### Added

- Function rename package android project.

## [0.0.5] - 2023-01-24

### Added

- Function rename package android project.

[Unreleased]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.1.8...v1.0.0
[0.1.8]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.1.7...v0.1.8
[0.1.7]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.1.6...v0.1.7
[0.1.6]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.1.5...v0.1.6
[0.1.5]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.1.4...v0.1.5
[0.1.4]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.0.9...v0.1.0
[0.0.9]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.0.8...v0.0.9
[0.0.8]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.0.6...v0.0.7
[0.0.6]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.0.5-alpha...v0.0.6
[0.0.5]: https://github.com/nguyenphuc22/Android-Package-Renamer/commits/v0.0.5
[0.0.5-alpha]: https://github.com/nguyenphuc22/Android-Package-Renamer/compare/v0.0.5...v0.0.5-alpha
