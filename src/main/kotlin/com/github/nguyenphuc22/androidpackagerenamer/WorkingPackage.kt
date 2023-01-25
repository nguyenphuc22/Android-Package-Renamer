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

class WorkingPackage : AnAction() {
    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project

        e.presentation.isEnabled = project != null
    }
    override fun actionPerformed(e: AnActionEvent) {
        val oldPackageName = getPackageName(e.project!!)

        val newPackageName = Messages.showInputDialog(e.project,null,"Your Package Name",null,oldPackageName,null)



        if (newPackageName != null) {
            // Rename Android Manifest.xml
            renameManifest(e.project!!,newPackageName,oldPackageName)
            val newFolderName = newPackageName.replace(oldChar =  '.', newChar = '/')
            // Create new folder follow new packageName
            val vfs = VirtualFileManager.getInstance().getFileSystem("file")
            // source
            val sourceDir = vfs.findFileByPath(e.project!!.basePath + "/app/src/main/java")
            val newFolder: VirtualFile = VfsUtil.createDirectoryIfMissing(sourceDir, newFolderName)
            // Android TestFolder
            val androidTestDir = vfs.findFileByPath(e.project!!.basePath + "/app/src/androidTest/java")
            val newAndroidTestFolder: VirtualFile = VfsUtil.createDirectoryIfMissing(androidTestDir, newFolderName)
            // TestFolder
            val testDir = vfs.findFileByPath(e.project!!.basePath + "/app/src/test/java")
            val newTestFolder: VirtualFile = VfsUtil.createDirectoryIfMissing(testDir, newFolderName)

            // Move all files in old folder to new folder
            // Source
            var oldPathFolder = e.project!!.basePath + "/app/src/main/java/" + oldPackageName.replace('.','/')
            var newPathFolder = e.project!!.basePath + "/app/src/main/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
            // Rename each file
            //renameEachFile(newPathFolder,newPackageName,oldPackageName)
            val vfs1 = VirtualFileManager.getInstance().getFileSystem("file")
            val folder = vfs1.findFileByPath(newPathFolder)
            renameEachFile(folder!!,newPackageName,oldPackageName)
            // Android TestFolder
            oldPathFolder = e.project!!.basePath + "/app/src/androidTest/java/" + oldPackageName.replace('.','/')
            newPathFolder = e.project!!.basePath + "/app/src/androidTest/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
            // Rename each file
            renameEachFile(newPathFolder,newPackageName,oldPackageName)

            // TestFolder
            oldPathFolder = e.project!!.basePath + "/app/src/test/java/" + oldPackageName.replace('.','/')
            newPathFolder = e.project!!.basePath + "/app/src/test/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
            // Rename each file
            renameEachFile(newPathFolder,newPackageName,oldPackageName)

            // Rename applicationId in build.gradle
            renameGradle(e.project!!,oldPackageName,newPackageName)
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
            val data = FileDocumentManager.getInstance().getDocument(sourceDir)!!.text
            if (data.contains("package=")) {
                val replace = data.replace(oldPackage,newPackage)
                WriteAction.run<IOException> {
                    VfsUtil.saveText(sourceDir, replace)
                }
            }
        }
    }

    fun moveFilesOldToNewFolder(oldPath : String, newPath : String) {
        val oldFolder = VfsUtil.findFileByIoFile(File(oldPath), true)
        val newFolder = VfsUtil.findFileByIoFile(File(newPath), true)
        for (file in oldFolder!!.children) {
            file.move(this, newFolder!!)
        }
        oldFolder.parent.delete(this)
    }

    fun renameEachFile(pathString : String,newPackage: String,oldPackage: String) {
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")
        val folder = vfs.findFileByPath(pathString)
        for (file in folder!!.children) {
            if (file.isDirectory) {
                renameEachFile(file.path,newPackage,oldPackage)
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