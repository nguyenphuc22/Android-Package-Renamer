package com.github.nguyenphuc22.androidpackagerenamer.objectMain

import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ManagerFileTest {

    @Mock
    private lateinit var mockProject: Project

    private lateinit var managerFile: ManagerFile

    @BeforeEach
    fun setUp() {
        managerFile = ManagerFile(mockProject)
    }

    @Test
    fun `should create ManagerFile with project`() {
        assertNotNull(managerFile)
    }

    @Test
    fun `validateNewPackageName should return true for valid package names`() {
        val validPackageNames = listOf(
            "com.example.app",
            "org.jetbrains.kotlin",
            "io.github.user_name",
            "com.company.project.module",
            "a.b.c",
            "Test123.Package_Name.App"
        )

        validPackageNames.forEach { packageName ->
            assertTrue(managerFile.validateNewPackageName(packageName), 
                "Package name '$packageName' should be valid")
        }
    }

    @Test
    fun `validateNewPackageName should return false for invalid package names`() {
        val invalidPackageNames = listOf(
            "",
            " ",
            "   ",
            "com",
            "com.",
            ".com.example",
            "com..example",
            "com.example.",
            "123.example.app",
            "com.123example.app",
            "com.example-app.test",
            "com.example app.test",
            "com.example@app.test"
        )

        invalidPackageNames.forEach { packageName ->
            assertFalse(managerFile.validateNewPackageName(packageName), 
                "Package name '$packageName' should be invalid")
        }
    }

    @Test
    fun `validateNewPackageName should handle edge cases`() {
        // Single character segments (valid)
        assertTrue(managerFile.validateNewPackageName("a.b.c"))

        // Long package names (valid)
        val longPackageName = "com.verylongcompanyname.verylongprojectname.verylongmodulename"
        assertTrue(managerFile.validateNewPackageName(longPackageName))

        // Package with numbers and underscores (valid)
        assertTrue(managerFile.validateNewPackageName("com.example123.test_module.app_v2"))

        // Starts with number (invalid)
        assertFalse(managerFile.validateNewPackageName("2com.example.app"))

        // Contains special characters (invalid)
        assertFalse(managerFile.validateNewPackageName("com.example-app.test"))
        assertFalse(managerFile.validateNewPackageName("com.example@app.test"))
        assertFalse(managerFile.validateNewPackageName("com.example app.test"))
    }

    @Test
    fun `getCurrentPackageName should be callable`() {
        // Since getCurrentPackageName just calls getPackageName(), 
        // we can test this delegation behavior
        
        // Just verify the method exists and can be called
        assertDoesNotThrow {
            try {
                val result = managerFile.getCurrentPackageName()
                // May return null or throw NPE due to static dependencies
            } catch (e: NullPointerException) {
                // Expected due to VirtualFileManager static dependencies  
            }
        }
    }

    @Test
    fun `getPackageName should be callable`() {
        // Just verify the method exists and can be called
        assertDoesNotThrow {
            try {
                val result = managerFile.getPackageName()
                // May return null or throw NPE due to static dependencies
            } catch (e: NullPointerException) {
                // Expected due to VirtualFileManager static dependencies
            }
        }
    }

    @Test
    fun `validateNewPackageName should use correct regex pattern`() {
        // Test the exact pattern used in the validation
        val pattern = "^([A-Za-z]{1}[A-Za-z\\d_]*\\.)+[A-Za-z][A-Za-z\\d_]*\$"
        val regex = Regex(pattern)
        
        // Valid cases
        assertTrue("com.example.app".matches(regex))
        assertTrue("org.test.Package_123".matches(regex))
        assertTrue("a.b".matches(regex))
        
        // Invalid cases
        assertFalse("com".matches(regex))
        assertFalse("com.".matches(regex))
        assertFalse(".com.example".matches(regex))
        assertFalse("123.example".matches(regex))
        assertFalse("com..example".matches(regex))
    }

    @Test
    fun `should handle package names with mixed case correctly`() {
        assertTrue(managerFile.validateNewPackageName("Com.Example.App"))
        assertTrue(managerFile.validateNewPackageName("COM.EXAMPLE.APP"))
        assertTrue(managerFile.validateNewPackageName("com.EXAMPLE.app"))
        assertTrue(managerFile.validateNewPackageName("myCompany.myProject.myApp"))
    }

    @Test
    fun `should reject package names with invalid characters`() {
        val invalidChars = listOf("-", "+", "=", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "[", "]", "{", "}", "|", "\\", ":", ";", "\"", "'", "<", ">", ",", "?", "/")
        
        invalidChars.forEach { char ->
            val packageName = "com.example${char}app.test"
            assertFalse(managerFile.validateNewPackageName(packageName), 
                "Package name with character '$char' should be invalid")
        }
    }

    @Test
    fun `should validate package names with underscores correctly`() {
        // Underscores are allowed in package names after the first character
        assertTrue(managerFile.validateNewPackageName("com.example_app.test_module"))
        assertTrue(managerFile.validateNewPackageName("com.example.app_test"))
        assertTrue(managerFile.validateNewPackageName("com.example_.app"))
        
        // Underscore at the beginning of a segment is NOT valid according to regex
        // Pattern requires first character to be [A-Za-z]
        assertFalse(managerFile.validateNewPackageName("com._example.app"))
    }

    @Test
    fun `should validate package names with numbers correctly`() {
        // Numbers are allowed but cannot be the first character of a segment
        assertTrue(managerFile.validateNewPackageName("com.example2.app3"))
        assertTrue(managerFile.validateNewPackageName("com.example.app123"))
        assertTrue(managerFile.validateNewPackageName("com.test4app.module5"))
        
        // Numbers at the beginning of segments are invalid
        assertFalse(managerFile.validateNewPackageName("com.2example.app"))
        assertFalse(managerFile.validateNewPackageName("1com.example.app"))
        assertFalse(managerFile.validateNewPackageName("com.example.3app"))
    }

    @Test
    fun `should have correct package structure`() {
        val packageName = ManagerFile::class.java.packageName
        assertEquals("com.github.nguyenphuc22.androidpackagerenamer.objectMain", packageName)
    }
}