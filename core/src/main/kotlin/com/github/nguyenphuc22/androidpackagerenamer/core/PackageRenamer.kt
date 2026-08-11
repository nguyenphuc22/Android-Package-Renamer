package com.github.nguyenphuc22.androidpackagerenamer.core

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Comparator

data class RenameOptions(
    val cleanBuildDirectories: Boolean = true,
)

data class RenameResult(
    val movedFiles: Int = 0,
    val updatedFiles: Int = 0,
    val cleanedBuildDirectories: Int = 0,
)

/**
 * Pure, IDE-independent Android package renamer operating on a plain `java.nio.file.Path`
 * project directory. Can be reused by any JVM tooling (CLI, Gradle plugin, etc.).
 *
 * Ported from the IntelliJ plugin's `PsiRefactor.performAndroidPackageRefactoring` and friends.
 */
class PackageRenamer(
    private val projectDir: Path,
    private val options: RenameOptions = RenameOptions(),
) {

    fun currentPackageName(): String? {
        val manifest = readIfExists(AndroidProjectLayout.manifestFile(projectDir))
        val gradle = readIfExists(AndroidProjectLayout.buildGradleFile(projectDir))
        val gradleKts = readIfExists(AndroidProjectLayout.buildGradleKtsFile(projectDir))
        return PackageNameExtractor.extract(manifest, gradle, gradleKts)
    }

    /**
     * Updates only the Android-specific files (manifest, build.gradle applicationId,
     * namespace insertion, build directory cleanup).
     *
     * Used by the IntelliJ PSI path, which handles source and directory refactoring itself.
     */
    fun updateAndroidFilesOnly(newPackage: String) {
        updateManifestAndGradle(newPackage)
        if (options.cleanBuildDirectories) {
            cleanBuildDirectories(projectDir)
        }
    }

    /**
     * Performs the full Android package refactoring: moves package directories,
     * rewrites source/resource file contents and updates the Android files.
     */
    fun rename(oldPackage: String, newPackage: String): RenameResult {
        if (oldPackage.isBlank() || newPackage.isBlank()) {
            throw IllegalArgumentException("Package names cannot be empty")
        }
        if (oldPackage == newPackage) {
            throw IllegalArgumentException("New package name must be different from the old one")
        }
        if (!PackageNameValidator.isValid(oldPackage)) {
            throw IllegalArgumentException("Old package name '$oldPackage' is not a valid package name")
        }
        if (!PackageNameValidator.isValid(newPackage)) {
            throw IllegalArgumentException("New package name '$newPackage' is not a valid package name")
        }
        if (newPackage.startsWith("$oldPackage.")) {
            throw IllegalArgumentException(
                "New package name '$newPackage' extends the old package '$oldPackage', which is not supported"
            )
        }

        val oldPath = AndroidProjectLayout.packagePath(oldPackage)
        val newPath = AndroidProjectLayout.packagePath(newPackage)

        // The old package must actually exist: either in the source tree, or matching the
        // detected project package (for manifest/gradle-only projects). This prevents a partial
        // rename when an explicit --old-package does not match the project.
        val foundInSources = AndroidProjectLayout.sourceRoots(projectDir).any { root ->
            Files.isDirectory(root) && Files.isDirectory(root.resolve(oldPath))
        }
        if (!foundInSources && currentPackageName() != oldPackage) {
            throw IllegalArgumentException(
                "Package '$oldPackage' does not match the current package " +
                    "'${currentPackageName() ?: "unknown"}' and was not found in the source directories"
            )
        }

        updateManifestAndGradle(newPackage)

        var movedFiles = 0
        var updatedFiles = 0

        AndroidProjectLayout.sourceRoots(projectDir).forEach { sourceRoot ->
            if (Files.isDirectory(sourceRoot)) {
                val oldDir = sourceRoot.resolve(oldPath)
                if (Files.isDirectory(oldDir)) {
                    movedFiles += moveDirectory(oldDir, sourceRoot.resolve(newPath))
                }
            }
        }

        AndroidProjectLayout.sourceRoots(projectDir).forEach { sourceRoot ->
            val newDir = sourceRoot.resolve(newPath)
            if (Files.isDirectory(newDir)) {
                updatedFiles += rewriteFiles(newDir, oldPackage, newPackage)
            }
        }

        val resDir = AndroidProjectLayout.resDirectory(projectDir)
        if (Files.isDirectory(resDir)) {
            updatedFiles += rewriteFiles(resDir, oldPackage, newPackage)
        }

        val cleanedBuildDirectories = if (options.cleanBuildDirectories) cleanBuildDirectories(projectDir) else 0

        return RenameResult(
            movedFiles = movedFiles,
            updatedFiles = updatedFiles,
            cleanedBuildDirectories = cleanedBuildDirectories,
        )
    }

    private fun updateManifestAndGradle(newPackage: String) {
        val manifest = AndroidProjectLayout.manifestFile(projectDir)
        if (Files.exists(manifest)) {
            Files.writeString(manifest, TextRewriter.updateManifestPackage(Files.readString(manifest), newPackage))
        }

        val gradle = AndroidProjectLayout.buildGradleFile(projectDir)
        val kts = AndroidProjectLayout.buildGradleKtsFile(projectDir)
        val buildFile = when {
            Files.exists(gradle) -> gradle
            Files.exists(kts) -> kts
            else -> null
        }
        if (buildFile != null) {
            val isKotlinDsl = buildFile == kts
            var content = Files.readString(buildFile)
            content = TextRewriter.updateApplicationId(content, newPackage)
            content = TextRewriter.updateNamespace(content, newPackage, isKotlinDsl)
            Files.writeString(buildFile, content)
        }
    }

    private fun moveDirectory(oldDir: Path, newDir: Path): Int {
        Files.createDirectories(newDir)
        var moved = 0
        val children = Files.list(oldDir).use { it.toList() }
        children.forEach { child ->
            val target = newDir.resolve(child.fileName)
            if (Files.isDirectory(child)) {
                moved += moveDirectory(child, target)
            } else {
                Files.move(child, target, StandardCopyOption.REPLACE_EXISTING)
                moved++
            }
        }
        cleanupEmptyDirectories(oldDir)
        return moved
    }

    private fun cleanupEmptyDirectories(directory: Path) {
        if (Files.isDirectory(directory) && isEmpty(directory)) {
            Files.deleteIfExists(directory)
            val parent = directory.parent ?: return
            val parentName = parent.fileName.toString()
            if (parentName != "java" && parentName != "kotlin" && isEmpty(parent)) {
                cleanupEmptyDirectories(parent)
            }
        }
    }

    private fun isEmpty(directory: Path): Boolean =
        Files.list(directory).use { stream -> stream.findAny().isEmpty }

    private fun rewriteFiles(directory: Path, oldPackage: String, newPackage: String): Int {
        var updated = 0
        Files.walk(directory).use { stream ->
            stream.filter { Files.isRegularFile(it) }.forEach { file ->
                val extension = file.fileName.toString().substringAfterLast('.', "")
                if (extension in SUPPORTED_FILE_EXTENSIONS) {
                    val content = Files.readString(file)
                    if (content.contains(oldPackage)) {
                        Files.writeString(file, TextRewriter.replacePackageReferences(content, oldPackage, newPackage))
                        updated++
                    }
                }
            }
        }
        return updated
    }

    private fun cleanBuildDirectories(projectDir: Path): Int {
        var cleaned = 0
        AndroidProjectLayout.BUILD_DIRECTORIES.forEach { relative ->
            val dir = projectDir.resolve(relative)
            if (Files.isDirectory(dir)) {
                try {
                    deleteRecursively(dir)
                    cleaned++
                } catch (e: IOException) {
                    System.err.println("Warning: Could not delete build directory $dir: ${e.message}")
                }
            }
        }
        return cleaned
    }

    private fun deleteRecursively(dir: Path) {
        if (!Files.exists(dir)) return
        Files.walk(dir).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private fun readIfExists(path: Path): String? =
        if (Files.exists(path)) Files.readString(path) else null

    companion object {
        val SUPPORTED_FILE_EXTENSIONS = listOf("java", "kt", "xml")
    }
}
