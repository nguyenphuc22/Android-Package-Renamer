package com.github.nguyenphuc22.androidpackagerenamer

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

        val newPackageName = Messages.showInputDialog(e.project,null,"Your Package Name",null,oldPackageName,MyValidator())

        if (newPackageName != null) {
            // Rename Android Manifest.xml
            renameManifest(e.project!!,newPackageName,oldPackageName)
            val newFolderName = newPackageName.replace(oldChar =  '.', newChar = '/')
//          Create new folder follow new packageName
            val vfs = VirtualFileManager.getInstance().getFileSystem("file")
//          source
            val sourceDir = vfs.findFileByPath(e.project!!.basePath + "/app/src/main/java")
            VfsUtil.createDirectories(sourceDir!!.path + "/" + newFolderName)
//          Android TestFolder
            val androidTestDir = vfs.findFileByPath(e.project!!.basePath + "/app/src/androidTest/java")
            VfsUtil.createDirectories(androidTestDir!!.path + "/" + newFolderName)
//          TestFolder
            val testDir = vfs.findFileByPath(e.project!!.basePath + "/app/src/test/java")
            VfsUtil.createDirectories(testDir!!.path + "/" + newFolderName)

//          Move all files in old folder to new folder
//          Source
            var oldPathFolder = e.project!!.basePath + "/app/src/main/java/" + oldPackageName.replace('.','/')
            var newPathFolder = e.project!!.basePath + "/app/src/main/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
//          Rename each file
            var folder = vfs.findFileByPath(newPathFolder)
            renameEachFile(folder!!,newPackageName,oldPackageName)
//          Android TestFolder
            oldPathFolder = e.project!!.basePath + "/app/src/androidTest/java/" + oldPackageName.replace('.','/')
            newPathFolder = e.project!!.basePath + "/app/src/androidTest/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
//          Rename each file
            folder = vfs.findFileByPath(newPathFolder)
            renameEachFile(folder!!,newPackageName,oldPackageName)
//          TestFolder
            oldPathFolder = e.project!!.basePath + "/app/src/test/java/" + oldPackageName.replace('.','/')
            newPathFolder = e.project!!.basePath + "/app/src/test/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
//          Rename each file
            folder = vfs.findFileByPath(newPathFolder)
            renameEachFile(folder!!,newPackageName,oldPackageName)
//          Rename applicationId in build.gradle
            renameGradle(e.project!!,oldPackageName,newPackageName)

            // Display Success
            Messages.showInfoMessage("Your package ${newPackageName}. \n Don't forget Sync Project with Gradle Files.","Rename Package Success")
        }

    }

    fun getPackageName(project: Project): String {
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")
        val sourceDir = vfs.findFileByPath(project.basePath + "/app/build.gradle")
        if (sourceDir != null) {
            val dataGradle = FileDocumentManager.getInstance().getDocument(sourceDir)!!.text
            val packageName = dataGradle.substringAfter("applicationId").substringAfter("\"").substringBefore("\"")
            return packageName
        }
        return "com.example.myapplication"
    }

    fun renameManifest(project: Project, newPackage: String, oldPackage: String) {
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")
        val sourceDir = vfs.findFileByPath(project.basePath + "/app/src/main/AndroidManifest.xml")
        if (sourceDir != null) {
            var data = FileDocumentManager.getInstance().getDocument(sourceDir)!!.text
            if (data.contains("package=")) {
                data = data.replace(oldPackage,newPackage)
            } else {
                val builder = StringBuilder(data)
                builder.insert(builder.indexOf("<manifest") + "<manifest".length," package=\"${newPackage}\" \n \t")
                data = builder.toString()
            }
            WriteAction.run<IOException> {
                VfsUtil.saveText(sourceDir, data)
            }
        }
    }

    fun moveFilesOldToNewFolder(oldPath : String, newPath : String) {
        val oldFolder = VfsUtil.findFileByIoFile(File(oldPath), true)
        val newFolder = VfsUtil.findFileByIoFile(File(newPath), true)
        for (file in oldFolder!!.children) {
            WriteAction.run<IOException> {
                file.move(this, newFolder!!)
            }
        }
        WriteAction.run<IOException> {
            deleteOldFolder(oldFolder)
        }
    }

    fun deleteOldFolder(oldFolder:VirtualFile) {
        if (oldFolder.children.isEmpty()) {
            val parent = oldFolder.parent
            oldFolder.delete(this)
            deleteOldFolder(parent)
        }
    }

    fun renameEachFile(virtualFile: VirtualFile, newPackage: String, oldPackage: String) {
        val root = VfsUtil.getChildren(virtualFile)
        for (file in root) {
            if (file.isDirectory) {
                renameEachFile(file,newPackage,oldPackage)
            } else {
                val data = FileDocumentManager.getInstance().getDocument(file)!!.text
                if (data.contains(oldPackage)) {
                    val replace = data.replace(oldPackage,newPackage)
                    WriteAction.run<IOException> {
                        VfsUtil.saveText(file, replace)
                    }
                }
            }
        }
    }

    fun renameGradle(project: Project, oldPackage: String, newPackage: String) {
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")
        val sourceGradle = vfs.findFileByPath(project.basePath + "/app/build.gradle")
        if (sourceGradle != null) {
            val data = FileDocumentManager.getInstance().getDocument(sourceGradle)!!.text
            if (data.contains(oldPackage)) {
                val replace = data.replace(oldPackage,newPackage)
                WriteAction.run<IOException> {
                    VfsUtil.saveText(sourceGradle, replace)
                }
            }
        }
    }
}