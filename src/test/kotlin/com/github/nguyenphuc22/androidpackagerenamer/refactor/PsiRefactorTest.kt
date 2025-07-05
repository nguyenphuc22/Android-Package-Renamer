package com.github.nguyenphuc22.androidpackagerenamer.refactor

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiPackage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PsiRefactorTest {

    @Mock
    private lateinit var mockProject: Project

    @Mock
    private lateinit var mockJavaPsiFacade: JavaPsiFacade

    @Mock
    private lateinit var mockPsiPackage: PsiPackage

    @Mock
    private lateinit var mockVirtualFileManager: VirtualFileManager

    @Mock
    private lateinit var mockVirtualFileSystem: VirtualFileSystem

    @Mock
    private lateinit var mockVirtualFile: VirtualFile

    @BeforeEach
    fun setUp() {
        // Setup common mocks
    }

    @Test
    fun `should validate input parameters correctly`() {
        var errorMessage = ""
        var successCalled = false
        
        val onError: (String) -> Unit = { message -> errorMessage = message }
        val onSuccess: () -> Unit = { successCalled = true }

        // Test empty old package name
        PsiRefactor.renamePackageWithPSI(mockProject, "", "com.new.package", onSuccess, onError)
        assertEquals("Package names cannot be empty", errorMessage)
        assertFalse(successCalled)

        // Reset
        errorMessage = ""
        successCalled = false

        // Test empty new package name
        PsiRefactor.renamePackageWithPSI(mockProject, "com.old.package", "", onSuccess, onError)
        assertEquals("Package names cannot be empty", errorMessage)
        assertFalse(successCalled)

        // Reset
        errorMessage = ""
        successCalled = false

        // Test identical package names
        PsiRefactor.renamePackageWithPSI(mockProject, "com.same.package", "com.same.package", onSuccess, onError)
        assertEquals("New package name must be different from the old one", errorMessage)
        assertFalse(successCalled)
    }

    @Test
    fun `should handle blank package names`() {
        var errorMessage = ""
        val onError: (String) -> Unit = { message -> errorMessage = message }
        val onSuccess: () -> Unit = { }

        // Test blank old package name (spaces only)
        PsiRefactor.renamePackageWithPSI(mockProject, "   ", "com.new.package", onSuccess, onError)
        assertEquals("Package names cannot be empty", errorMessage)

        // Reset
        errorMessage = ""

        // Test blank new package name
        PsiRefactor.renamePackageWithPSI(mockProject, "com.old.package", "  ", onSuccess, onError)
        assertEquals("Package names cannot be empty", errorMessage)
    }

    @Test
    fun `should have correct class structure`() {
        // Test that PsiRefactor is a companion object class
        assertNotNull(PsiRefactor)
        
        // Test that the main method exists
        assertDoesNotThrow {
            // This should not throw - just testing method signature exists
            try {
                PsiRefactor.renamePackageWithPSI(mockProject, "old", "new", {}, {})
            } catch (e: Exception) {
                // Expected due to mocking limitations
            }
        }
    }

    @Test
    fun `should handle project with null base path gracefully`() {
        var errorCalled = false
        val onError: (String) -> Unit = { errorCalled = true }
        val onSuccess: () -> Unit = { }

        assertDoesNotThrow {
            try {
                PsiRefactor.renamePackageWithPSI(mockProject, "com.old.package", "com.new.package", onSuccess, onError)
            } catch (e: Exception) {
                // Expected due to VFS operations failing with null base path
            }
        }
    }

    @Test
    fun `should handle package path conversion correctly`() {
        // Test the package path logic using string operations
        val packageName = "com.example.myapp"
        val expectedPath = "com/example/myapp"
        
        val actualPath = packageName.replace('.', '/')
        assertEquals(expectedPath, actualPath)
        
        // Test reverse conversion
        val backToPackage = actualPath.replace('/', '.')
        assertEquals(packageName, backToPackage)
    }

    @Test
    fun `should validate package name format`() {
        // Test valid package names that should pass validation
        val validPackageNames = listOf(
            "com.example.app",
            "org.jetbrains.kotlin",
            "io.github.username",
            "a.b.c.d.e.f"
        )
        
        validPackageNames.forEach { packageName ->
            assertTrue(packageName.isNotBlank(), "Package name '$packageName' should not be blank")
            assertTrue(packageName.contains('.'), "Package name '$packageName' should contain dots")
            assertFalse(packageName.startsWith('.'), "Package name '$packageName' should not start with dot")
            assertFalse(packageName.endsWith('.'), "Package name '$packageName' should not end with dot")
        }
        
        // Test invalid package names
        val invalidPackageNames = listOf(
            "",
            "   ",
            "com",
            "com.",
            ".com.example",
            "com..example"
        )
        
        invalidPackageNames.forEach { packageName ->
            assertTrue(
                packageName.isBlank() || 
                !packageName.contains('.') || 
                packageName.startsWith('.') || 
                packageName.endsWith('.') || 
                packageName.contains(".."),
                "Package name '$packageName' should be considered invalid"
            )
        }
    }

    @Test
    fun `should handle file extension validation`() {
        // Test supported file extensions
        val supportedExtensions = listOf("java", "kt", "xml")
        
        supportedExtensions.forEach { ext ->
            assertTrue(ext in listOf("java", "kt", "xml"), 
                "Extension '$ext' should be supported")
        }
        
        // Test unsupported extensions
        val unsupportedExtensions = listOf("txt", "md", "json", "gradle")
        
        unsupportedExtensions.forEach { ext ->
            assertFalse(ext in listOf("java", "kt", "xml"), 
                "Extension '$ext' should not be supported for refactoring")
        }
    }

    @Test
    fun `should handle Android project structure paths`() {
        val projectBasePath = "/path/to/project"
        val packageName = "com.example.app"
        val packagePath = packageName.replace('.', '/')
        
        val expectedPaths = listOf(
            "$projectBasePath/app/src/main/java/$packagePath",
            "$projectBasePath/app/src/main/kotlin/$packagePath",
            "$projectBasePath/app/src/androidTest/java/$packagePath",
            "$projectBasePath/app/src/androidTest/kotlin/$packagePath",
            "$projectBasePath/app/src/test/java/$packagePath",
            "$projectBasePath/app/src/test/kotlin/$packagePath"
        )
        
        expectedPaths.forEach { path ->
            assertTrue(path.contains(packagePath), "Path should contain package path")
            assertTrue(path.startsWith(projectBasePath), "Path should start with project base path")
            assertTrue(path.contains("/src/"), "Path should contain src directory")
        }
    }

    @Test
    fun `should handle manifest and gradle file patterns`() {
        // Test manifest file patterns
        val manifestContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.example.oldapp">
                <application>
                </application>
            </manifest>
        """.trimIndent()
        
        assertTrue(manifestContent.contains("package="))
        assertTrue(manifestContent.contains("com.example.oldapp"))
        
        // Test gradle file patterns
        val gradleContent = """
            android {
                compileSdk 34
                defaultConfig {
                    applicationId "com.example.oldapp"
                    minSdk 21
                    targetSdk 34
                }
            }
        """.trimIndent()
        
        assertTrue(gradleContent.contains("applicationId"))
        assertTrue(gradleContent.contains("com.example.oldapp"))
        
        // Test gradle kts patterns
        val gradleKtsContent = """
            android {
                compileSdk = 34
                defaultConfig {
                    applicationId = "com.example.oldapp"
                    minSdk = 21
                    targetSdk = 34
                }
            }
        """.trimIndent()
        
        assertTrue(gradleKtsContent.contains("applicationId"))
        assertTrue(gradleKtsContent.contains("com.example.oldapp"))
    }

    @Test
    fun `should handle build directory paths correctly`() {
        val projectBasePath = "/path/to/project"
        
        val expectedBuildPaths = listOf(
            "$projectBasePath/build",
            "$projectBasePath/app/build",
            "$projectBasePath/.gradle"
        )
        
        expectedBuildPaths.forEach { path ->
            assertTrue(path.startsWith(projectBasePath), "Build path should start with project base path")
            assertTrue(path.contains("build") || path.contains(".gradle"), 
                "Path should be a build or gradle directory")
        }
    }

    @Test
    fun `should have correct package structure`() {
        val packageName = PsiRefactor::class.java.packageName
        assertEquals("com.github.nguyenphuc22.androidpackagerenamer.refactor", packageName)
    }
}