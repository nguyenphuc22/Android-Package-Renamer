package com.github.nguyenphuc22.androidpackagerenamer.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PackageNameValidatorTest {

    @Test
    fun `valid package names should be accepted`() {
        val valid = listOf(
            "com.example.app",
            "org.jetbrains.kotlin",
            "io.github.user_name",
            "com.company.project.module",
            "a.b.c",
            "Test123.Package_Name.App",
            "com.example123.test_module.app_v2",
            "COM.EXAMPLE.APP",
            "com.verylongcompanyname.verylongprojectname.verylongmodulename",
        )
        valid.forEach { name -> assertTrue(PackageNameValidator.isValid(name), "'$name' should be valid") }
    }

    @Test
    fun `invalid package names should be rejected`() {
        val invalid = listOf(
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
            "com.example@app.test",
            "2com.example.app",
            "com.example.3app",
            "com._example.app",
        )
        invalid.forEach { name -> assertFalse(PackageNameValidator.isValid(name), "'$name' should be invalid") }
    }

    @Test
    fun `pattern should be the canonical package name regex`() {
        assertTrue(PackageNameValidator.PATTERN.matches("com.example.app"))
        assertFalse(PackageNameValidator.PATTERN.matches("com"))
        assertFalse(PackageNameValidator.PATTERN.matches("123.example"))
    }
}
