package com.github.nguyenphuc22.androidpackagerenamer

import com.github.nguyenphuc22.androidpackagerenamer.objectMain.ContentNotification
import com.github.nguyenphuc22.androidpackagerenamer.objectMain.InfoProject
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

            val info = InfoProject(newPackageName,oldPackageName,getModeDatading(e.project!!))

            // Rename Android Manifest.xml
            renameManifest(e.project!!,info.packageNameNew,info.packageNameOld)
            val newFolderName = info.packageNameNew.replace(oldChar =  '.', newChar = '/')
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
            var oldPathFolder = e.project!!.basePath + "/app/src/main/java/" + info.packageNameOld.replace('.','/')
            var newPathFolder = e.project!!.basePath + "/app/src/main/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
//          Rename each file
            var folder = vfs.findFileByPath(newPathFolder)
            renameEachFile(folder!!,info.packageNameNew,info.packageNameOld)
//          Android TestFolder
            oldPathFolder = e.project!!.basePath + "/app/src/androidTest/java/" + info.packageNameOld.replace('.','/')
            newPathFolder = e.project!!.basePath + "/app/src/androidTest/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
//          Rename each file
            folder = vfs.findFileByPath(newPathFolder)
            renameEachFile(folder!!,info.packageNameNew,info.packageNameOld)
//          TestFolder
            oldPathFolder = e.project!!.basePath + "/app/src/test/java/" + info.packageNameOld.replace('.','/')
            newPathFolder = e.project!!.basePath + "/app/src/test/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
//          Rename each file
            folder = vfs.findFileByPath(newPathFolder)
            renameEachFile(folder!!,info.packageNameNew,info.packageNameOld)

            if (info.isDataBindingMode) {
                newPathFolder = e.project!!.basePath + "/app/src/main/res"
                folder = vfs.findFileByPath(newPathFolder)
                folder?.let {
                    renameEachFile(it,info.packageNameNew,info.packageNameOld)
                }
            }

//          Rename applicationId in build.gradle
            renameGradle(e.project!!,info.packageNameOld,info.packageNameNew)

            // Delete Build
            val pathBuild = e.project!!.basePath + "/build"
            val folderBuild = VfsUtil.findFileByIoFile(File(pathBuild), true)
            folderBuild?.let {
                it.delete(this)
            }
            // Delete app/build
            val pathAppBuild = e.project!!.basePath + "/app/build"
            val folderAppBuild = VfsUtil.findFileByIoFile(File(pathAppBuild), true)
            folderAppBuild?.let {
                it.delete(this)
            }
            // .gradle
            val pathGradle = e.project!!.basePath + "/.gradle"
            val folderGradle = VfsUtil.findFileByIoFile(File(pathGradle), true)
            folderGradle?.let {
                it.delete(this)
            }
            // Display Success
            Messages.showInfoMessage(ContentNotification.CONTENT_SUCCESS,ContentNotification.SUCCESS)
        }

    }

    fun getPackageName(project: Project): String {
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")
        val manifest = vfs.findFileByPath(project.basePath + "/app/src/main/AndroidManifest.xml")
        manifest?.let { manifestVir ->
            val dataManifest = FileDocumentManager.getInstance().getDocument(manifestVir)!!.text
            if (dataManifest.contains("package")) {
                val packageName = dataManifest.substringAfter("package=").substringAfter("\"").substringBefore("\"")
                return packageName
            } else {
                val sourceDir = vfs.findFileByPath(project.basePath + "/app/build.gradle")
                sourceDir?.let {
                    val dataGradle = FileDocumentManager.getInstance().getDocument(it)!!.text
                    val packageName = dataGradle.substringAfter("applicationId").substringAfter("\"").substringBefore("\"")
                    return packageName
                }
            }
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
            if (file.path != newFolder!!.path) {
                WriteAction.run<IOException> {
                    file.move(this, newFolder!!)
                }
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
                FileDocumentManager.getInstance().getDocument(file)?.let {
                    val data = it.text
                    if (data.contains(oldPackage)) {
                        val replace = data.replace(oldPackage,newPackage)
                        WriteAction.run<IOException> {
                            VfsUtil.saveText(file, replace)
                        }
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

    fun getModeDatading(project: Project) : Boolean {
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")
        val sourceGradle = vfs.findFileByPath(project.basePath + "/app/build.gradle")
        if (sourceGradle != null) {
            val dataGradle = FileDocumentManager.getInstance().getDocument(sourceGradle)!!.text
            if (dataGradle.contains("dataBinding"))
                return true
        }
        return false
    }
}