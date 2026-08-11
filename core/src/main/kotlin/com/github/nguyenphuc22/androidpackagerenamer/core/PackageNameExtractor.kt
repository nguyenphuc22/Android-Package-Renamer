package com.github.nguyenphuc22.androidpackagerenamer.core

/**
 * Extracts the current application/package name from Android project file contents.
 *
 * Pure string parsing extracted from the duplicated logic in
 * `WorkingPackage.getPackageName` and `ManagerFile.getPackageName`.
 */
object PackageNameExtractor {

    fun fromManifest(manifestContent: String?): String? {
        if (manifestContent == null || !manifestContent.contains("package=")) return null
        return manifestContent
            .substringAfter("package=")
            .substringAfter("\"")
            .substringBefore("\"")
            .takeIf { it.isNotBlank() }
    }

    fun fromBuildGradle(gradleContent: String?): String? {
        if (gradleContent == null || !gradleContent.contains("applicationId")) return null
        return gradleContent
            .substringAfter("applicationId")
            .substringAfter("\"")
            .substringBefore("\"")
            .takeIf { it.isNotBlank() }
    }

    /**
     * Priority: AndroidManifest.xml `package` attribute, then `build.gradle` `applicationId`,
     * then `build.gradle.kts` `applicationId`.
     */
    fun extract(manifestContent: String?, gradleContent: String?, gradleKtsContent: String?): String? =
        fromManifest(manifestContent) ?: fromBuildGradle(gradleContent) ?: fromBuildGradle(gradleKtsContent)
}
