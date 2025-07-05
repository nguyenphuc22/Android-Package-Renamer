package com.github.nguyenphuc22.androidpackagerenamer.objectMain

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
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

    fun validateNewPackageName(name: String): Boolean {
        val pattern = "^([A-Za-z]{1}[A-Za-z\\d_]*\\.)+[A-Za-z][A-Za-z\\d_]*\$"
        return name.isNotEmpty() && name.isNotBlank() && name.matches(Regex(pattern))
    }

    fun addNamespaceIfNotExist(newPackageName: String) {
        var gradleKotlin = false
        val vfs = VirtualFileManager.getInstance().getFileSystem("file")

        // Kiểm tra build.gradle
        var buildGradle = vfs.findFileByPath("${project.basePath}/app/build.gradle")

        if (buildGradle == null) {
            buildGradle = vfs.findFileByPath("${project.basePath}/app/build.gradle.kts")
            gradleKotlin = true
        }

        if (buildGradle != null) {
            val content = FileDocumentManager.getInstance().getDocument(buildGradle)!!.text

            // Nếu không chứa namespace
            if (!content.contains("namespace")) {
                // Tìm vị trí mở đầu của thẻ android {
                val androidIndex = content.indexOf("android {")
                if (androidIndex != -1) {

                    val builder = StringBuilder(content)
                    if (gradleKotlin) {
                        builder.insert(content.indexOf("android {") + "android {".length,"\n \tnamespace = \"${newPackageName}\"")
                    } else {
                        builder.insert(content.indexOf("android {") + "android {".length,"\n \tnamespace '${newPackageName}'")
                    }

                    WriteAction.run<java.io.IOException> {
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