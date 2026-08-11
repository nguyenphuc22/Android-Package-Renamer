# Android Package Renamer

![Build](https://github.com/nguyenphuc22/Android-Package-Renamer/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/20919.svg)](https://plugins.jetbrains.com/plugin/20919)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/20919.svg)](https://plugins.jetbrains.com/plugin/20919)
[![BuymeCoffee](https://badgen.net/badge/BuymeCoffee/PhucVR/yellow/?icon=buymeacoffee)](https://www.buymeacoffee.com/phucvr)
![Logo](https://github.com/nguyenphuc22/Android-Package-Renamer/blob/main/Android_Package_Renamer.png)

<!-- Plugin description -->

The Android Package Renamer plugin "Safe And Fast Android Rename" allows users to safely and quickly change the package name of an Android project. This plugin uses a series of automated refactoring steps to ensure that all references to the old package name are updated throughout the project, without breaking the code or causing errors.

## What the plugin can do

This plugin can rename packages in android project, here are a few specific cases (it can do more)::

- com.android.example -> org.nickseven.product
- com.android.example -> org.nickseven
- com.android.example -> org.nickseven.product.native
- com.android.etc.nick.d -> org.android.otd.nick.d
- ....etc

## Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Android Package Renamer"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/nguyenphuc22/Android-Package-Renamer/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Usage

1. **Open** Your Project.
2. Click -&gt; **File** -&gt; **Rename Package**
3. **Input the Package** you want to change.
4. Click **Ok**.
5. **Sync Project with Gradle Files** or **Invalidate Caches**

## Working on project using

### Gradle

- :heavy_check_mark: Gradle Groovy
- :heavy_check_mark: Gradle Kotlin

## Report an issue with Android Package Renamer

To report a specific problem or feature request, open a new issue on Github. For questions, suggestions, or anything else, email phucvr.opensource@gmai.com

## Author

[PhucVR on Twitter](https://twitter.com/phuc_vr)

[PhucVR on GitHub](https://github.com/nguyenphuc22)

<!-- Plugin description end -->

## Core module & CLI

The renaming engine lives in the [core](core/) module: a pure Kotlin/JVM library with **no IntelliJ
Platform dependency**. It can be reused by any JVM tooling (terminal scripts, Gradle plugins, CI bots, ...).

Build and run the command-line tool:

```bash
./gradlew :core:installDist
core/build/install/package-renamer-core/bin/package-renamer-core \
  --project-dir /path/to/android-project \
  --new-package com.example.newname
```

The CLI detects the current package automatically (from `AndroidManifest.xml` or `build.gradle(.kts)`)
and performs the same refactoring as the IDE plugin. Useful options:

- `--old-package <name>` — explicit old package (otherwise auto-detected)
- `--no-clean` — keep the `build/` directories
- `--dry-run` — preview changes on a temporary copy without touching the project
- `-h, --help` — show all options

Run `./gradlew :core:test` for the core unit and integration tests, which run on real Android
project fixtures (`core/src/test/resources/fixtures`).

## License

Apache 2.0. See the [LICENSE](https://github.com/nguyenphuc22/Android-Package-Renamer/blob/main/LICENSE.md) file for details.

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template