package com.github.nguyenphuc22.androidpackagerenamer.objectMain

import com.github.nguyenphuc22.androidpackagerenamer.core.PackageNameExtractor
import com.github.nguyenphuc22.androidpackagerenamer.core.PackageNameValidator
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager

class ManagerFile(private val project: Project) {

    fun getCurrentPackageName(): String? = getPackageName()

    fun getPackageName(): String? {
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")
        val basePath = project.basePath ?: return null

        val manifestText = vfs.findFileByPath("$basePath/app/src/main/AndroidManifest.xml")
            ?.let { FileDocumentManager.getInstance().getDocument(it)?.text }
        manifestText?.let { PackageNameExtractor.fromManifest(it)?.let { manifestPackage -> return manifestPackage } }

        val gradleText = vfs.findFileByPath("$basePath/app/build.gradle")
            ?.let { FileDocumentManager.getInstance().getDocument(it)?.text }
        gradleText?.let { PackageNameExtractor.fromBuildGradle(it)?.let { gradlePackage -> return gradlePackage } }

        val gradleKtsText = vfs.findFileByPath("$basePath/app/build.gradle.kts")
            ?.let { FileDocumentManager.getInstance().getDocument(it)?.text }
        return gradleKtsText?.let { PackageNameExtractor.fromBuildGradle(it) }
    }

    fun validateNewPackageName(name: String): Boolean = PackageNameValidator.isValid(name)
}
