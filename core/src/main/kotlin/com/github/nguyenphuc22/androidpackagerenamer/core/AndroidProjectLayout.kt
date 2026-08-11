package com.github.nguyenphuc22.androidpackagerenamer.core

import java.nio.file.Path

/**
 * Describes the layout of a standard single-`app`-module Android project,
 * mirroring the paths hard-coded in the original IntelliJ plugin.
 */
object AndroidProjectLayout {

    const val APP_MODULE = "app"
    const val MANIFEST_RELATIVE_PATH = "src/main/AndroidManifest.xml"
    const val BUILD_GRADLE_FILE = "build.gradle"
    const val BUILD_GRADLE_KTS_FILE = "build.gradle.kts"

    val SOURCE_ROOTS: List<String> = listOf(
        "src/main/java",
        "src/main/kotlin",
        "src/androidTest/java",
        "src/androidTest/kotlin",
        "src/test/java",
        "src/test/kotlin",
    )

    val BUILD_DIRECTORIES: List<String> = listOf(
        "build",
        "app/build",
        ".gradle",
    )

    fun appModuleDir(projectDir: Path): Path = projectDir.resolve(APP_MODULE)

    fun manifestFile(projectDir: Path): Path = appModuleDir(projectDir).resolve(MANIFEST_RELATIVE_PATH)

    fun buildGradleFile(projectDir: Path): Path = appModuleDir(projectDir).resolve(BUILD_GRADLE_FILE)

    fun buildGradleKtsFile(projectDir: Path): Path = appModuleDir(projectDir).resolve(BUILD_GRADLE_KTS_FILE)

    fun resDirectory(projectDir: Path): Path = appModuleDir(projectDir).resolve("src/main/res")

    fun sourceRoots(projectDir: Path): List<Path> =
        SOURCE_ROOTS.map { appModuleDir(projectDir).resolve(it) }

    fun packagePath(packageName: String): String = packageName.replace('.', '/')
}
