package com.github.nguyenphuc22.androidpackagerenamer

import com.github.nguyenphuc22.androidpackagerenamer.objectMain.ContentNotification
import com.github.nguyenphuc22.androidpackagerenamer.objectMain.InfoProject
import com.github.nguyenphuc22.androidpackagerenamer.objectMain.ManagerFile
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File
import java.io.IOException
import java.lang.StringBuilder

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
            manager.changePackageName(
                newPackageName,
                onSuccess =  {
                    Messages.showInfoMessage(ContentNotification.CONTENT_SUCCESS,ContentNotification.SUCCESS)
                },
                onError =  {
                    Messages.showInfoMessage(ContentNotification.CONTENT_GET_PACKAGE_NAME_FAIL,ContentNotification.FAIL)
                }
            )
        }
    }

    fun getPackageName(project: Project): String? {
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")
        val manifest = vfs.findFileByPath(project.basePath + "/app/src/main/AndroidManifest.xml")
        manifest?.let { manifestVir ->
            val dataManifest = FileDocumentManager.getInstance().getDocument(manifestVir)!!.text
            if (dataManifest.contains("package")) {
                val packageName = dataManifest.substringAfter("package=").substringAfter("\"").substringBefore("\"")
                return packageName
            } else {
                var sourceDir = vfs.findFileByPath(project.basePath + "/app/build.gradle")
                sourceDir?.let {
                    val dataGradle = FileDocumentManager.getInstance().getDocument(it)!!.text
                    if (dataGradle.contains("applicationId")) {
                        val packageName = dataGradle.substringAfter("applicationId").substringAfter("\"").substringBefore("\"")
                        return packageName
                    }
                }

                sourceDir = vfs.findFileByPath(project.basePath + "/app/build.gradle.kts")
                sourceDir?.let {
                    val dataGradle = FileDocumentManager.getInstance().getDocument(it)!!.text
                    if (dataGradle.contains("applicationId")) {
                        val packageName = dataGradle.substringAfter("applicationId").substringAfter("\"").substringBefore("\"")
                        return packageName
                    }
                }
            }
        }

        return null
    }
}