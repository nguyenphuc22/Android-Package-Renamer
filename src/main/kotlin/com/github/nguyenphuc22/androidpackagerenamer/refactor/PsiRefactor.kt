package com.github.nguyenphuc22.androidpackagerenamer.refactor

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.refactoring.RefactoringFactory
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.IOException

class PsiRefactor {
    companion object {
        /**
         * Performs safe package renaming using PSI (Program Structure Interface).
         * This function replaces the manual refactoring logic with IntelliJ's built-in refactoring system.
         * It automatically handles:
         * 1. Safe renaming in source code
         * 2. Directory movement
         * 3. Preview window for user confirmation
         * 4. Integration with Undo/Redo system
         */
        fun renamePackageWithPSI(project: Project, oldPackageName: String, newPackageName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
            try {
                // Validate inputs
                if (oldPackageName.isBlank() || newPackageName.isBlank()) {
                    onError("Package names cannot be empty")
                    return
                }
                
                if (oldPackageName == newPackageName) {
                    onError("New package name must be different from the old one")
                    return
                }

                // Find the PsiPackage object from the old name
                val javaPsiFacade = JavaPsiFacade.getInstance(project)
                val psiPackage = javaPsiFacade.findPackage(oldPackageName)

                if (psiPackage == null) {
                    // Try to find package directories manually in Android project structure
                    val packageDirs = findPackageDirectories(project, oldPackageName)
                    if (packageDirs.isEmpty()) {
                        onError("Package '$oldPackageName' not found in the project structure. Make sure the package exists in your Java/Kotlin source files.")
                        return
                    }
                    
                    // Use fallback refactoring for Android projects
                    performAndroidPackageRefactoring(project, oldPackageName, newPackageName, onSuccess, onError)
                    return
                }

                // Check if target package already exists
                val targetPackage = javaPsiFacade.findPackage(newPackageName)
                if (targetPackage != null && targetPackage.directories.isNotEmpty()) {
                    onError("Package '$newPackageName' already exists in the project")
                    return
                }

                // Update Android-specific files before PSI refactoring
                try {
                    updateAndroidFiles(project, oldPackageName, newPackageName)
                } catch (e: Exception) {
                    onError("Error updating Android files: ${e.message}")
                    return
                }

                // Create a "Rename" refactoring process
                val renameRefactoring = RefactoringFactory.getInstance(project).createRename(psiPackage, newPackageName)

                // Wrap the execution in a Command for Undo capability
                CommandProcessor.getInstance().executeCommand(
                    project,
                    {
                        try {
                            // Run refactoring. This will automatically show preview window.
                            renameRefactoring.run()
                            onSuccess()
                        } catch (e: Exception) {
                            onError("Error during refactoring execution: ${e.message}")
                        }
                    },
                    "Rename Package to $newPackageName",
                    null
                )
            } catch (e: Exception) {
                onError("Unexpected error during PSI refactoring: ${e.message}")
            }
        }

        /**
         * Updates Android-specific files that PSI refactoring doesn't handle automatically
         */
        private fun updateAndroidFiles(project: Project, oldPackageName: String, newPackageName: String) {
            WriteAction.runAndWait<IOException> {
                // Update AndroidManifest.xml
                updateAndroidManifest(project, oldPackageName, newPackageName)
                
                // Update build.gradle files
                updateBuildGradle(project, oldPackageName, newPackageName)
                
                // Add namespace if not exist
                addNamespaceIfNotExist(project, newPackageName)
                
                // Clean build directories
                cleanBuildDirectories(project)
            }
        }

        private fun updateAndroidManifest(project: Project, oldPackageName: String, newPackageName: String) {
            val vfs = VirtualFileManager.getInstance().getFileSystem("file")
            val manifest = vfs.findFileByPath("${project.basePath}/app/src/main/AndroidManifest.xml")
            
            if (manifest == null) {
                throw IllegalStateException("AndroidManifest.xml not found at expected location")
            }
            
            val document = FileDocumentManager.getInstance().getDocument(manifest)
            if (document == null) {
                throw IllegalStateException("Cannot read AndroidManifest.xml document")
            }
            
            var content = document.text
            if (content.contains("package=")) {
                content = content.replace(oldPackageName, newPackageName)
            } else {
                val builder = StringBuilder(content)
                val manifestIndex = builder.indexOf("<manifest")
                if (manifestIndex == -1) {
                    throw IllegalStateException("Invalid AndroidManifest.xml format: <manifest> tag not found")
                }
                val insertPos = manifestIndex + "<manifest".length
                builder.insert(insertPos, " package=\"$newPackageName\"")
                content = builder.toString()
            }
            VfsUtil.saveText(manifest, content)
        }

        private fun updateBuildGradle(project: Project, oldPackageName: String, newPackageName: String) {
            val vfs = VirtualFileManager.getInstance().getFileSystem("file")
            var buildGradle = vfs.findFileByPath("${project.basePath}/app/build.gradle")
            
            if (buildGradle == null) {
                buildGradle = vfs.findFileByPath("${project.basePath}/app/build.gradle.kts")
            }
            
            if (buildGradle == null) {
                throw IllegalStateException("build.gradle or build.gradle.kts not found in app directory")
            }
            
            val document = FileDocumentManager.getInstance().getDocument(buildGradle)
            if (document == null) {
                throw IllegalStateException("Cannot read build.gradle document")
            }
            
            var content = document.text
            if (content.contains("applicationId")) {
                try {
                    val currentAppId = content.substringAfter("applicationId").substringAfter("\"").substringBefore("\"")
                    if (currentAppId.isNotBlank()) {
                        content = content.replace(currentAppId, newPackageName)
                        VfsUtil.saveText(buildGradle, content)
                    }
                } catch (e: Exception) {
                    throw IllegalStateException("Error parsing applicationId from build.gradle: ${e.message}")
                }
            }
        }

        private fun addNamespaceIfNotExist(project: Project, newPackageName: String) {
            val vfs = VirtualFileManager.getInstance().getFileSystem("file")
            var buildGradle = vfs.findFileByPath("${project.basePath}/app/build.gradle")
            var isKotlin = false
            
            if (buildGradle == null) {
                buildGradle = vfs.findFileByPath("${project.basePath}/app/build.gradle.kts")
                isKotlin = true
            }
            
            buildGradle?.let {
                val document = FileDocumentManager.getInstance().getDocument(it)
                document?.let { doc ->
                    var content = doc.text
                    if (!content.contains("namespace")) {
                        val androidIndex = content.indexOf("android {")
                        if (androidIndex != -1) {
                            val builder = StringBuilder(content)
                            val insertPos = androidIndex + "android {".length
                            val namespaceDeclaration = if (isKotlin) {
                                "\n    namespace = \"$newPackageName\""
                            } else {
                                "\n    namespace '$newPackageName'"
                            }
                            builder.insert(insertPos, namespaceDeclaration)
                            VfsUtil.saveText(it, builder.toString())
                        }
                    }
                }
            }
        }

        private fun cleanBuildDirectories(project: Project) {
            val buildPaths = listOf(
                "${project.basePath}/build",
                "${project.basePath}/app/build",
                "${project.basePath}/.gradle"
            )
            
            buildPaths.forEach { path ->
                try {
                    val buildDir = VfsUtil.findFileByIoFile(java.io.File(path), true)
                    buildDir?.delete(null)
                } catch (e: Exception) {
                    // Log warning but don't fail the whole process
                    println("Warning: Could not delete build directory $path: ${e.message}")
                }
            }
        }

        /**
         * Finds package directories in Android project structure
         */
        private fun findPackageDirectories(project: Project, packageName: String): List<VirtualFile> {
            val vfs = VirtualFileManager.getInstance().getFileSystem("file")
            val packagePath = packageName.replace('.', '/')
            val directories = mutableListOf<VirtualFile>()
            
            val sourcePaths = listOf(
                "${project.basePath}/app/src/main/java/$packagePath",
                "${project.basePath}/app/src/main/kotlin/$packagePath",
                "${project.basePath}/app/src/androidTest/java/$packagePath",
                "${project.basePath}/app/src/androidTest/kotlin/$packagePath",
                "${project.basePath}/app/src/test/java/$packagePath",
                "${project.basePath}/app/src/test/kotlin/$packagePath"
            )
            
            sourcePaths.forEach { path ->
                vfs.findFileByPath(path)?.let { dir ->
                    if (dir.isDirectory && dir.children.isNotEmpty()) {
                        directories.add(dir)
                    }
                }
            }
            
            return directories
        }

        /**
         * Performs Android-specific package refactoring when PSI refactoring is not available
         */
        private fun performAndroidPackageRefactoring(
            project: Project, 
            oldPackageName: String, 
            newPackageName: String, 
            onSuccess: () -> Unit, 
            onError: (String) -> Unit
        ) {
            try {
                CommandProcessor.getInstance().executeCommand(
                    project,
                    {
                        WriteAction.runAndWait<IOException> {
                            // Update Android-specific files
                            updateAndroidFiles(project, oldPackageName, newPackageName)
                            
                            // Perform directory-based refactoring
                            performDirectoryRefactoring(project, oldPackageName, newPackageName)
                            
                            // Update source files
                            updateSourceFiles(project, oldPackageName, newPackageName)
                        }
                        onSuccess()
                    },
                    "Rename Package to $newPackageName",
                    null
                )
            } catch (e: Exception) {
                onError("Error during Android package refactoring: ${e.message}")
            }
        }

        /**
         * Performs directory-based refactoring for Android projects
         */
        private fun performDirectoryRefactoring(project: Project, oldPackageName: String, newPackageName: String) {
            val vfs = VirtualFileManager.getInstance().getFileSystem("file")
            val oldPath = oldPackageName.replace('.', '/')
            val newPath = newPackageName.replace('.', '/')
            
            val sourceDirs = listOf(
                "${project.basePath}/app/src/main/java",
                "${project.basePath}/app/src/main/kotlin",
                "${project.basePath}/app/src/androidTest/java",
                "${project.basePath}/app/src/androidTest/kotlin",
                "${project.basePath}/app/src/test/java",
                "${project.basePath}/app/src/test/kotlin"
            )
            
            sourceDirs.forEach { sourceDir ->
                val oldDir = vfs.findFileByPath("$sourceDir/$oldPath")
                if (oldDir != null && oldDir.isDirectory) {
                    // Create new directory structure
                    val newDir = VfsUtil.createDirectories("$sourceDir/$newPath")
                    
                    // Move files
                    oldDir.children.forEach { file ->
                        if (file.path != newDir.path) {
                            file.move(null, newDir)
                        }
                    }
                    
                    // Clean up old directory structure
                    cleanupOldDirectories(oldDir)
                }
            }
        }

        /**
         * Updates source files with new package declarations
         */
        private fun updateSourceFiles(project: Project, oldPackageName: String, newPackageName: String) {
            val vfs = VirtualFileManager.getInstance().getFileSystem("file")
            val newPath = newPackageName.replace('.', '/')
            
            val sourceDirs = listOf(
                "${project.basePath}/app/src/main/java/$newPath",
                "${project.basePath}/app/src/main/kotlin/$newPath",
                "${project.basePath}/app/src/androidTest/java/$newPath",
                "${project.basePath}/app/src/androidTest/kotlin/$newPath",
                "${project.basePath}/app/src/test/java/$newPath",
                "${project.basePath}/app/src/test/kotlin/$newPath"
            )
            
            sourceDirs.forEach { dirPath ->
                val dir = vfs.findFileByPath(dirPath)
                dir?.let { updateFilesInDirectory(it, oldPackageName, newPackageName) }
            }
            
            // Also update resource files
            val resDir = vfs.findFileByPath("${project.basePath}/app/src/main/res")
            resDir?.let { updateFilesInDirectory(it, oldPackageName, newPackageName) }
        }

        /**
         * Recursively updates files in a directory
         */
        private fun updateFilesInDirectory(directory: VirtualFile, oldPackageName: String, newPackageName: String) {
            directory.children.forEach { file ->
                if (file.isDirectory) {
                    updateFilesInDirectory(file, oldPackageName, newPackageName)
                } else if (file.extension in listOf("java", "kt", "xml")) {
                    val document = FileDocumentManager.getInstance().getDocument(file)
                    document?.let { doc ->
                        val content = doc.text
                        if (content.contains(oldPackageName)) {
                            val newContent = content.replace(oldPackageName, newPackageName)
                            VfsUtil.saveText(file, newContent)
                        }
                    }
                }
            }
        }

        /**
         * Cleans up old directory structure
         */
        private fun cleanupOldDirectories(directory: VirtualFile) {
            if (directory.children.isEmpty()) {
                val parent = directory.parent
                directory.delete(null)
                parent?.let { 
                    if (it.name != "java" && it.name != "kotlin" && it.children.isEmpty()) {
                        cleanupOldDirectories(it)
                    }
                }
            }
        }
    }
}