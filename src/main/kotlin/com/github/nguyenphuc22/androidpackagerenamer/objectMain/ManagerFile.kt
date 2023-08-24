package com.github.nguyenphuc22.androidpackagerenamer.objectMain

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File
import java.io.IOException
import java.lang.StringBuilder

class ManagerFile(private val project: Project) {

    fun getCurrentPackageName(): String? {
        // Sử dụng hàm getPackageName() hiện tại
        // để lấy package name
        return getPackageName()
    }

    // Thêm hàm này
    fun getPackageName(): String? {
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")
        val manifest = vfs.findFileByPath(project.basePath + "/app/src/main/AndroidManifest.xml")
        var packageName = ""
         manifest?.let { manifestVir ->
            val dataManifest = FileDocumentManager.getInstance().getDocument(manifestVir)!!.text
            if (dataManifest.contains("package")) {
                packageName = dataManifest.substringAfter("package=").substringAfter("\"").substringBefore("\"")
                return packageName
            } else {
                var sourceDir = vfs.findFileByPath(project.basePath + "/app/build.gradle")
                sourceDir?.let {
                    val dataGradle = FileDocumentManager.getInstance().getDocument(it)!!.text
                    if (dataGradle.contains("applicationId")) {
                        packageName = dataGradle.substringAfter("applicationId").substringAfter("\"").substringBefore("\"")
                        return packageName
                    }
                }

                sourceDir = vfs.findFileByPath(project.basePath + "/app/build.gradle.kts")
                sourceDir?.let {
                    val dataGradle = FileDocumentManager.getInstance().getDocument(it)!!.text
                    if (dataGradle.contains("applicationId")) {
                        packageName = dataGradle.substringAfter("applicationId").substringAfter("\"").substringBefore("\"")
                        return packageName
                    }
                }
            }
        }
        return packageName
    }

    // Các phương thức khác

    fun validateNewPackageName(name: String): Boolean {
        val pattern = "^([A-Za-z]{1}[A-Za-z\\d_]*\\.)+[A-Za-z][A-Za-z\\d_]*\$"
        return name.isNotEmpty() && name.isNotBlank() && name.matches(Regex(pattern))
    }

    fun changePackageName(newName: String, onSuccess : () -> Unit, onError : () -> Unit) {
        // Gọi các hàm khác trong file để thực hiện
        // thay đổi package name
        val oldName = getCurrentPackageName()
        if (oldName != null) {
            val info = InfoProject(newName,oldName,getModeData(project!!))

            val newFolderName = info.packageNameNew.replace(oldChar =  '.', newChar = '/')
//          Create new folder follow new packageName
            val vfs = VirtualFileManager.getInstance().getFileSystem("file")
//          source
            val sourceDir = vfs.findFileByPath(project!!.basePath + "/app/src/main/java")
            sourceDir?.let {
                VfsUtil.createDirectories(it.path + "/" + newFolderName)
            }

//          Android TestFolder
            val androidTestDir = vfs.findFileByPath(project!!.basePath + "/app/src/androidTest/java")
            androidTestDir?.let {
                VfsUtil.createDirectories(it.path + "/" + newFolderName)
            }

//          TestFolder
            val testDir = vfs.findFileByPath(project!!.basePath + "/app/src/test/java")
            testDir?.let {
                VfsUtil.createDirectories(it.path + "/" + newFolderName)
            }

//          Move all files in old folder to new folder
//          Source
            var oldPathFolder = project!!.basePath + "/app/src/main/java/" + info.packageNameOld.replace('.','/')
            var newPathFolder = project!!.basePath + "/app/src/main/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
//          Rename each file
            var folder = vfs.findFileByPath(newPathFolder)
            folder?.let {
                renameEachFile(it,info.packageNameNew,info.packageNameOld)
            }

//          Android TestFolder
            oldPathFolder = project!!.basePath + "/app/src/androidTest/java/" + info.packageNameOld.replace('.','/')
            newPathFolder = project!!.basePath + "/app/src/androidTest/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
//          Rename each file
            folder = vfs.findFileByPath(newPathFolder)
            folder?.let {
                renameEachFile(it,info.packageNameNew,info.packageNameOld)
            }

//          TestFolder
            oldPathFolder = project!!.basePath + "/app/src/test/java/" + info.packageNameOld.replace('.','/')
            newPathFolder = project!!.basePath + "/app/src/test/java/" + newFolderName
            moveFilesOldToNewFolder(oldPathFolder,newPathFolder)
//          Rename each file
            folder = vfs.findFileByPath(newPathFolder)
            folder?.let {
                renameEachFile(it,info.packageNameNew,info.packageNameOld)
            }

            newPathFolder = project!!.basePath + "/app/src/main/res"
            folder = vfs.findFileByPath(newPathFolder)
            folder?.let {
                renameEachFile(it,info.packageNameNew,info.packageNameOld)
            }

//          Rename applicationId in build.gradle
            renameGradle(project!!,info.packageNameOld,info.packageNameNew)

            addNamespaceIfNotExist(info.packageNameNew)

            // Delete Build
            val pathBuild = project!!.basePath + "/build"
            val folderBuild = VfsUtil.findFileByIoFile(File(pathBuild), true)
            folderBuild?.let {
                it.delete(this)
            }
            // Delete app/build
            val pathAppBuild = project!!.basePath + "/app/build"
            val folderAppBuild = VfsUtil.findFileByIoFile(File(pathAppBuild), true)
            folderAppBuild?.let {
                it.delete(this)
            }
            // .gradle
            val pathGradle = project!!.basePath + "/.gradle"
            val folderGradle = VfsUtil.findFileByIoFile(File(pathGradle), true)
            folderGradle?.let {
                it.delete(this)
            }

            onSuccess.invoke()
        } else {
            onError.invoke()
        }
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

    private fun moveFilesOldToNewFolder(oldPath : String, newPath : String) {
        val oldFolder = VfsUtil.findFileByIoFile(File(oldPath), true)
        val newFolder = VfsUtil.findFileByIoFile(File(newPath), true)
        if (oldFolder == null)
            return
        if (newFolder == null)
            return

        for (file in oldFolder.children) {
            if (file.path != newFolder.path) {
                WriteAction.run<IOException> {
                    file.move(this, newFolder)
                }
            }
        }
        WriteAction.run<IOException> {
            deleteOldFolder(oldFolder)
        }
    }

    private fun deleteOldFolder(oldFolder: VirtualFile) {
        if (oldFolder.children.isEmpty()) {
            val parent = oldFolder.parent
            oldFolder.delete(this)
            deleteOldFolder(parent)
        }
    }

    private fun renameEachFile(virtualFile: VirtualFile, newPackage: String, oldPackage: String) {
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

    private fun renameGradle(project: Project, oldPackage: String, newPackage: String) {
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")
        var sourceGradle = vfs.findFileByPath(project.basePath + "/app/build.gradle")
        if (sourceGradle == null) {
            sourceGradle = vfs.findFileByPath(project.basePath + "/app/build.gradle.kts")
        }
        sourceGradle?.let {
            val data = FileDocumentManager.getInstance().getDocument(it)!!.text
            val applicationID = data.substringAfter("applicationId").substringAfter("\"").substringBefore("\"")
            if (data.contains(applicationID)) {
                val replace = data.replace(applicationID,newPackage)
                WriteAction.run<IOException> {
                    VfsUtil.saveText(it, replace)
                }
            }
        }
    }

    fun addNamespaceIfNotExist(newPackageName: String) {

        val vfs = VirtualFileManager.getInstance().getFileSystem("file")

        // Kiểm tra build.gradle
        var buildGradle = vfs.findFileByPath("${project.basePath}/app/build.gradle")

        if (buildGradle != null) {
            val content = FileDocumentManager.getInstance().getDocument(buildGradle)!!.text

            // Nếu không chứa namespace
            if (!content.contains("namespace")) {
                // Tìm vị trí mở đầu của thẻ android {
                val androidIndex = content.indexOf("android {")
                if (androidIndex != -1) {

                    val builder = StringBuilder(content)
                    builder.insert(content.indexOf("android {") + "android {".length,"\n \tnamespace '${newPackageName}'")

                    WriteAction.run<IOException> {
                        VfsUtil.saveText(buildGradle, builder.toString())
                    }
                }

            }

        } else {
            // Kiểm tra file build.gradle.kts
            // Thực hiện tương tự

        }

    }

    private fun getModeData(project: Project) : Boolean {
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