package com.github.nguyenphuc22.androidpackagerenamer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class MyValidatorTest {

    private lateinit var validator: MyValidator

    @BeforeEach
    fun setUp() {
        validator = MyValidator()
    }

    @Test
    fun `should have correct regex pattern`() {
        val expectedPattern = "^([A-Za-z]{1}[A-Za-z\\d_]*\\.)+[A-Za-z][A-Za-z\\d_]*\$"
        assertEquals(expectedPattern, validator.pattern)
    }

    @Test
    fun `checkInput should return true for valid package names`() {
        val validPackageNames = listOf(
            "com.example.app",
            "org.jetbrains.kotlin",
            "io.github.user_name",
            "com.company.project.module",
            "a.b.c",
            "Test123.Package_Name.App"
        )

        validPackageNames.forEach { packageName ->
            assertTrue(validator.checkInput(packageName), "Package name '$packageName' should be valid")
        }
    }

    @Test
    fun `checkInput should return false for invalid package names`() {
        val invalidPackageNames = listOf(
            null,
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
            assertFalse(validator.checkInput(packageName), "Package name '$packageName' should be invalid")
        }
    }

    @Test
    fun `canClose should always return true`() {
        assertTrue(validator.canClose("valid.package.name"))
        assertTrue(validator.canClose("invalid"))
        assertTrue(validator.canClose(""))
        assertTrue(validator.canClose(null))
    }

    @Test
    fun `getErrorText should return null for valid package names`() {
        val validPackageNames = listOf(
            "com.example.app",
            "org.jetbrains.kotlin",
            "io.github.user_name"
        )

        validPackageNames.forEach { packageName ->
            assertNull(validator.getErrorText(packageName), "Valid package name '$packageName' should not have error text")
        }
    }

    @Test
    fun `getErrorText should return error message for invalid package names`() {
        val invalidPackageNames = listOf(
            "com",
            "com.",
            ".com.example",
            "123.example.app",
            "com.example-app.test"
        )

        invalidPackageNames.forEach { packageName ->
            assertEquals("Package name is not valid", validator.getErrorText(packageName), 
                "Invalid package name '$packageName' should return error message")
        }
    }

    @Test
    fun `getErrorText should return null for null input`() {
        assertNull(validator.getErrorText(null))
    }

    @Test
    fun `should validate edge cases correctly`() {
        // Single character segments (valid)
        assertTrue(validator.checkInput("a.b.c"))
        assertNull(validator.getErrorText("a.b.c"))

        // Long package names (valid)
        val longPackageName = "com.verylongcompanyname.verylongprojectname.verylongmodulename"
        assertTrue(validator.checkInput(longPackageName))
        assertNull(validator.getErrorText(longPackageName))

        // Package with numbers and underscores (valid)
        assertTrue(validator.checkInput("com.example123.test_module.app_v2"))
        assertNull(validator.getErrorText("com.example123.test_module.app_v2"))

        // Starts with number (invalid)
        assertFalse(validator.checkInput("2com.example.app"))
        assertEquals("Package name is not valid", validator.getErrorText("2com.example.app"))
    }
}