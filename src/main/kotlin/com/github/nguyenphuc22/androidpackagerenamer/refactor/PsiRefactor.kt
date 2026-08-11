package com.github.nguyenphuc22.androidpackagerenamer.refactor

import com.github.nguyenphuc22.androidpackagerenamer.core.PackageRenamer
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.JavaPsiFacade
import com.intellij.refactoring.RefactoringFactory
import java.io.IOException
import java.nio.file.Path

class PsiRefactor {
    companion object {
        /**
         * Performs safe package renaming using PSI (Program Structure Interface).
         *
         * The PSI-based `RefactoringFactory` path provides preview/undo support and handles
         * source code + directory refactoring. Android-specific file updates (AndroidManifest.xml,
         * build.gradle, namespace, build directories) and the fallback for non-PSI projects are
         * delegated to the IDE-independent [PackageRenamer] core.
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

                val projectDir = project.basePath
                if (projectDir == null) {
                    onError("Project base path is not available")
                    return
                }

                // Find the PsiPackage object from the old name
                val javaPsiFacade = JavaPsiFacade.getInstance(project)
                val psiPackage = javaPsiFacade.findPackage(oldPackageName)

                if (psiPackage == null) {
                    // Fallback: perform a file-system based refactoring using the core engine
                    performAndroidPackageRefactoring(project, projectDir, oldPackageName, newPackageName, onSuccess, onError)
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
                    WriteAction.runAndWait<IOException> {
                        PackageRenamer(Path.of(projectDir)).updateAndroidFilesOnly(newPackageName)
                    }
                    refreshVfs(projectDir)
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
         * Performs Android-specific package refactoring when PSI refactoring is not available,
         * using the pure [PackageRenamer] core engine.
         */
        private fun performAndroidPackageRefactoring(
            project: Project,
            projectDir: String,
            oldPackageName: String,
            newPackageName: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
        ) {
            try {
                WriteAction.runAndWait<IOException> {
                    PackageRenamer(Path.of(projectDir)).rename(oldPackageName, newPackageName)
                }
                refreshVfs(projectDir)
                onSuccess()
            } catch (e: Exception) {
                onError("Error during Android package refactoring: ${e.message}")
            }
        }

        /**
         * Refreshes the IntelliJ VFS so that the changes written by the core engine
         * (outside of VFS) are picked up by the IDE.
         */
        private fun refreshVfs(projectDir: String) {
            val localFileSystem = LocalFileSystem.getInstance()
            if (localFileSystem.refreshAndFindFileByPath(projectDir) == null) {
                localFileSystem.refresh(false)
            }
        }
    }
}
