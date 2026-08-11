package com.github.nguyenphuc22.androidpackagerenamer.core

/**
 * Pure text transformations for Android project files.
 * Ported from `PsiRefactor.updateAndroidManifest`, `updateBuildGradle` and `addNamespaceIfNotExist`.
 */
object TextRewriter {

    fun replacePackageReferences(content: String, oldPackage: String, newPackage: String): String =
        content.replace(oldPackage, newPackage)

    /**
     * Updates the `package` attribute of an `AndroidManifest.xml` file, or inserts it
     * right after the `<manifest` tag when it is absent.
     */
    fun updateManifestPackage(content: String, newPackage: String): String {
        if (content.contains("package=")) {
            val current = content.substringAfter("package=").substringAfter("\"").substringBefore("\"")
            if (current.isBlank()) {
                throw IllegalStateException("Cannot parse the package attribute from AndroidManifest.xml")
            }
            return content.replace(current, newPackage)
        }
        val manifestIndex = content.indexOf("<manifest")
        if (manifestIndex == -1) {
            throw IllegalStateException("Invalid AndroidManifest.xml format: <manifest> tag not found")
        }
        val insertPos = manifestIndex + "<manifest".length
        return StringBuilder(content).insert(insertPos, " package=\"$newPackage\"").toString()
    }

    /**
     * Updates the `applicationId` value of a `build.gradle` or `build.gradle.kts` file.
     */
    fun updateApplicationId(content: String, newPackage: String): String {
        if (!content.contains("applicationId")) return content
        val currentAppId = content.substringAfter("applicationId").substringAfter("\"").substringBefore("\"")
        if (currentAppId.isBlank()) return content
        return content.replace(currentAppId, newPackage)
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
     * Updates an existing `namespace` declaration in a `build.gradle`/`build.gradle.kts` file,
     * or inserts a new one when it is missing.
     */
    fun updateNamespace(content: String, newPackage: String, isKotlinDsl: Boolean): String {
        val replacement = if (isKotlinDsl) "namespace = \"$newPackage\"" else "namespace '$newPackage'"
        val pattern = if (isKotlinDsl) Regex("""namespace\s*=\s*"[^"]*"""") else Regex("""namespace\s*'[^']*'""")
        val updated = pattern.replaceFirst(content, replacement)
        return if (updated != content) updated else addNamespaceIfMissing(content, newPackage, isKotlinDsl)
    }
}
