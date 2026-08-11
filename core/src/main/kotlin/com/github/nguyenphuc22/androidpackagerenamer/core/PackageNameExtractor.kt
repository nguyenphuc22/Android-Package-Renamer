package com.github.nguyenphuc22.androidpackagerenamer.core

/**
 * Extracts the current application/package name from Android project file contents.
 *
 * Pure string parsing extracted from the duplicated logic in
 * `WorkingPackage.getPackageName` and `ManagerFile.getPackageName`.
 */
object PackageNameExtractor {

    private val MANIFEST_PACKAGE = Regex("""package\s*=\s*"([^"]*)"""")
    private val APPLICATION_ID = Regex("""applicationId\b\s*=?\s*"([^"]*)"""")
    private val NAMESPACE = Regex("""namespace\b\s*=?\s*['"]([^'"]*)['"]""")

    fun fromManifest(manifestContent: String?): String? {
        if (manifestContent == null) return null
        return MANIFEST_PACKAGE.find(manifestContent)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }

    fun fromBuildGradle(gradleContent: String?): String? {
        if (gradleContent == null) return null
        return APPLICATION_ID.find(gradleContent)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }

    /**
     * Extracts the `namespace` declaration, used as a fallback for library modules
     * that do not declare an `applicationId`.
     */
    fun fromNamespace(gradleContent: String?): String? {
        if (gradleContent == null) return null
        return NAMESPACE.find(gradleContent)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }

    /**
     * Priority: AndroidManifest.xml `package` attribute, then `build.gradle` `applicationId`,
     * then `build.gradle` `namespace`, then `build.gradle.kts` `applicationId`/`namespace`.
     */
    fun extract(manifestContent: String?, gradleContent: String?, gradleKtsContent: String?): String? =
        fromManifest(manifestContent)
            ?: fromBuildGradle(gradleContent)
            ?: fromNamespace(gradleContent)
            ?: fromBuildGradle(gradleKtsContent)
            ?: fromNamespace(gradleKtsContent)
}
