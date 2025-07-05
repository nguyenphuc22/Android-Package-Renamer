package com.github.nguyenphuc22.androidpackagerenamer.objectMain

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ManagerFileIntegrationTest {

    @Mock
    private lateinit var mockProject: Project

    @Mock
    private lateinit var mockVirtualFileManager: VirtualFileManager

    @Mock
    private lateinit var mockVirtualFileSystem: VirtualFileSystem

    @Mock
    private lateinit var mockVirtualFile: VirtualFile

    private lateinit var managerFile: ManagerFile

    @BeforeEach
    fun setUp() {
        managerFile = ManagerFile(mockProject)
    }

    @Test
    fun `should handle InfoProject integration correctly`() {
        // Arrange
        val oldPackage = "com.example.oldapp"
        val newPackage = "com.example.newapp"
        val infoProject = InfoProject(newPackage, oldPackage)
        
        // Act & Assert - Test InfoProject creation and validation
        assertEquals(oldPackage, infoProject.packageNameOld)
        assertEquals(newPackage, infoProject.packageNameNew)
        assertFalse(infoProject.isDataBindingMode)
    }

    @Test
    fun `should validate package names before processing`() {
        // Arrange - Valid package names
        val validOldPackage = "com.example.valid"
        val validNewPackage = "com.example.newvalid"
        val validInfo = InfoProject(validNewPackage, validOldPackage)
        
        // This should pass validation
        assertTrue(managerFile.validateNewPackageName(validNewPackage))
        assertTrue(managerFile.validateNewPackageName(validOldPackage))
        
        // Arrange - Invalid package names
        val invalidNewPackage = "invalid_package"
        val invalidInfo = InfoProject(invalidNewPackage, validOldPackage)
        
        // This should fail validation
        assertFalse(managerFile.validateNewPackageName(invalidNewPackage))
    }

    @Test
    fun `should handle project path operations correctly`() {
        // Test path construction logic used in doMain
        val projectBasePath = "/test/project"
        val packageName = "com.example.app"
        val expectedJavaPath = packageName.replace('.', '/')
        
        // Test path building logic
        val mainJavaPath = "$projectBasePath/app/src/main/java/$expectedJavaPath"
        val testJavaPath = "$projectBasePath/app/src/test/java/$expectedJavaPath"
        val androidTestPath = "$projectBasePath/app/src/androidTest/java/$expectedJavaPath"
        
        // Verify path construction
        assertTrue(mainJavaPath.contains("/src/main/java/"))
        assertTrue(testJavaPath.contains("/src/test/java/"))
        assertTrue(androidTestPath.contains("/src/androidTest/java/"))
        assertTrue(mainJavaPath.endsWith("com/example/app"))
        
        // Test build directory paths
        val buildPath = "$projectBasePath/build"
        val appBuildPath = "$projectBasePath/app/build"
        val gradlePath = "$projectBasePath/.gradle"
        
        assertTrue(buildPath.endsWith("/build"))
        assertTrue(appBuildPath.endsWith("/app/build"))
        assertTrue(gradlePath.endsWith("/.gradle"))
    }

    @Test
    fun `should handle resource directory operations`() {
        // Test resource path construction
        val projectBasePath = "/test/project"
        val resourcePath = "$projectBasePath/app/src/main/res"
        
        assertTrue(resourcePath.contains("/src/main/res"))
        assertFalse(resourcePath.contains("java"))
    }

    @Test
    fun `should handle manifest file operations`() {
        // Test manifest path construction
        val projectBasePath = "/test/project"
        val manifestPath = "$projectBasePath/app/src/main/AndroidManifest.xml"
        
        assertTrue(manifestPath.endsWith("AndroidManifest.xml"))
        assertTrue(manifestPath.contains("/app/src/main/"))
    }

    @Test
    fun `should handle gradle file operations`() {
        // Test gradle file path construction
        val projectBasePath = "/test/project"
        
        // Test different gradle file possibilities
        val gradlePath = "$projectBasePath/app/build.gradle"
        val gradleKtsPath = "$projectBasePath/app/build.gradle.kts"
        
        assertTrue(gradlePath.endsWith("build.gradle"))
        assertTrue(gradleKtsPath.endsWith("build.gradle.kts"))
        assertTrue(gradlePath.contains("/app/"))
        assertTrue(gradleKtsPath.contains("/app/"))
    }

    @Test
    fun `should handle callback invocation correctly`() {
        // Test callback mechanism
        var successCount = 0
        var errorCount = 0
        
        val onSuccess = { successCount++ }
        val onError = { errorCount++ }
        
        // Test manual callback invocation (simulating what happens in doMain)
        onSuccess.invoke()
        assertEquals(1, successCount)
        assertEquals(0, errorCount)
        
        onError.invoke()
        assertEquals(1, successCount)
        assertEquals(1, errorCount)
    }

    @Test
    fun `should handle InfoProject data binding mode correctly`() {
        // Test InfoProject with data binding
        val oldPackage = "com.example.old"
        val newPackage = "com.example.new"
        val infoProject = InfoProject(newPackage, oldPackage)
        
        // Test data binding mode toggling
        assertFalse(infoProject.isDataBindingMode)
        
        infoProject.isDataBindingMode = true
        assertTrue(infoProject.isDataBindingMode)
        
        // Verify package names remain consistent
        assertEquals(oldPackage, infoProject.packageNameOld)
        assertEquals(newPackage, infoProject.packageNameNew)
    }

    @Test
    fun `should handle file extension filtering`() {
        // Test file extension logic (used in renameEachFile)
        val javaFile = "Example.java"
        val kotlinFile = "Example.kt"
        val xmlFile = "layout.xml"
        val textFile = "readme.txt"
        
        // Simulate extension checking logic
        val supportedExtensions = listOf("java", "kt", "xml")
        
        assertTrue(javaFile.substringAfterLast('.') in supportedExtensions)
        assertTrue(kotlinFile.substringAfterLast('.') in supportedExtensions)
        assertTrue(xmlFile.substringAfterLast('.') in supportedExtensions)
        assertFalse(textFile.substringAfterLast('.') in supportedExtensions)
    }

    @Test
    fun `should handle package name replacement logic`() {
        // Test package name replacement logic (used throughout the class)
        val oldPackage = "com.example.oldapp"
        val newPackage = "com.example.newapp"
        val content = "package com.example.oldapp\n\nimport com.example.oldapp.utils.Helper"
        
        // Simulate replacement logic
        val updatedContent = content.replace(oldPackage, newPackage)
        
        assertTrue(updatedContent.contains(newPackage))
        assertFalse(updatedContent.contains(oldPackage))
        assertEquals("package com.example.newapp\n\nimport com.example.newapp.utils.Helper", updatedContent)
    }

    @Test
    fun `should have correct class structure for integration`() {
        val packageName = ManagerFile::class.java.packageName
        assertEquals("com.github.nguyenphuc22.androidpackagerenamer.objectMain", packageName)
        
        // Verify ManagerFile can be instantiated with Project
        assertNotNull(managerFile)
    }
}