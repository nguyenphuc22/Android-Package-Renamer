package com.github.nguyenphuc22.androidpackagerenamer

import com.github.nguyenphuc22.androidpackagerenamer.core.PackageNameExtractor
import com.github.nguyenphuc22.androidpackagerenamer.objectMain.ContentNotification
import com.github.nguyenphuc22.androidpackagerenamer.objectMain.ManagerFile
import com.github.nguyenphuc22.androidpackagerenamer.refactor.PsiRefactor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager

class WorkingPackage : AnAction() {
    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        e.presentation.isEnabled = project != null
    }
    override fun actionPerformed(e: AnActionEvent) {
        val oldPackageName = getPackageName(e.project!!)

        if (oldPackageName == null) {
            Messages.showInfoMessage(ContentNotification.CONTENT_GET_PACKAGE_NAME_FAIL,ContentNotification.GET_PACKAGE_NAME_FAIL)
            return
        }

        val newPackageName = Messages.showInputDialog(e.project,null,"Your Package Name",null,oldPackageName,MyValidator())

        val manager = ManagerFile(e.project!!)
        if (manager.validateNewPackageName(newPackageName!!)) {
            // Use PSI-based refactoring instead of manual approach
            PsiRefactor.renamePackageWithPSI(
                e.project!!,
                oldPackageName,
                newPackageName,
                onSuccess = {
                    println("Success")
                    Messages.showInfoMessage(ContentNotification.CONTENT_SUCCESS, ContentNotification.SUCCESS)
                },
                onError = { errorMessage ->
                    println("Fail: $errorMessage")
                    Messages.showInfoMessage(errorMessage, ContentNotification.FAIL)
                }
            )
        }
    }

    fun getPackageName(project: Project): String? {
        val basePath = project.basePath ?: return null
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")

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
}