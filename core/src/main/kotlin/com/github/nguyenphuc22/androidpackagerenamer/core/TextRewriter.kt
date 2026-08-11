package com.github.nguyenphuc22.androidpackagerenamer.core

/**
 * Pure text transformations for Android project files.
 * Ported from `PsiRefactor.updateAndroidManifest`, `updateBuildGradle` and `addNamespaceIfNotExist`.
 */
object TextRewriter {

    /**
     * Replaces whole-word occurrences of the old package, so identifiers that merely share a
     * prefix (e.g. `com.foo.appx`) are left untouched while sub-packages (`com.foo.app.core`)
     * are still renamed.
     */
    fun replacePackageReferences(content: String, oldPackage: String, newPackage: String): String {
        val pattern = Regex("\\b" + Regex.escape(oldPackage) + "\\b")
        return pattern.replace(content, newPackage)
    }

    /**
     * Updates the `package` attribute of an `AndroidManifest.xml` file.
     * When the attribute is absent (modern AGP 8+ projects) the manifest is left untouched,
     * since the namespace in `build.gradle(.kts)` is the source of truth.
     */
    fun updateManifestPackage(content: String, newPackage: String): String {
        if (!content.contains("package=")) return content
        val pattern = Regex("""package\s*=\s*"([^"]*)"""")
        val current = pattern.find(content)?.groupValues?.get(1)
        if (current.isNullOrBlank()) {
            throw IllegalStateException("Cannot parse the package attribute from AndroidManifest.xml")
        }
        return content.replace(current, newPackage)
    }

    /**
     * Updates the `applicationId` value of a `build.gradle` or `build.gradle.kts` file,
     * supporting both `applicationId "..."` (Groovy) and `applicationId = "..."` (Kotlin DSL)
     * and never matching `applicationIdSuffix`.
     */
    fun updateApplicationId(content: String, newPackage: String): String {
        val pattern = Regex("""applicationId\b\s*=?\s*"([^"]*)"""")
        return pattern.replace(content) { match ->
            val hasEquals = match.value.contains("=")
            if (hasEquals) "applicationId = \"$newPackage\"" else "applicationId \"$newPackage\""
        }
    }

    /**
     * Inserts a `namespace` declaration right after the `android {` block if it is missing.
     */
    fun addNamespaceIfMissing(content: String, newPackage: String, isKotlinDsl: Boolean): String {
        if (content.contains("namespace")) return content
        val androidIndex = content.indexOf("android {")
        if (androidIndex == -1) return content
        val declaration = if (isKotlinDsl) {
            "\n    namespace = \"$newPackage\""
        } else {
            "\n    namespace '$newPackage'"
        }
        return StringBuilder(content).insert(androidIndex + "android {".length, declaration).toString()
    }

    /**
     * Updates an existing `namespace` declaration (Groovy `namespace 'x'`/`namespace "x"` or
     * Kotlin DSL `namespace = "x"`), or inserts a new one when it is missing.
     */
    fun updateNamespace(content: String, newPackage: String, isKotlinDsl: Boolean): String {
        val pattern = Regex("""namespace\s*=?\s*['"][^'"]*['"]""")
        val match = pattern.find(content) ?: return addNamespaceIfMissing(content, newPackage, isKotlinDsl)
        val hasEquals = match.value.contains("=")
        val replacement = if (hasEquals) "namespace = \"$newPackage\"" else "namespace '$newPackage'"
        return content.replaceFirst(match.value, replacement)
    }
}
