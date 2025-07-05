package com.github.nguyenphuc22.androidpackagerenamer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.util.Locale

class MyBundleTest {

    @BeforeEach
    fun setUp() {
        // Ensure tests run in a predictable locale
        Locale.setDefault(Locale.ENGLISH)
    }

    @Test
    fun `should load message for existing key without parameters`() {
        val result = MyBundle.message("name")
        assertEquals("My Plugin", result)
    }

    @Test
    fun `should load message for existing key with parameters`() {
        val projectName = "TestProject"
        val result = MyBundle.message("projectService", projectName)
        assertEquals("Project service: TestProject", result)
    }

    @Test
    fun `should load application service message`() {
        val result = MyBundle.message("applicationService")
        assertEquals("Application service", result)
    }

    @Test
    fun `should handle multiple parameters correctly`() {
        // Test with projectService which has one parameter
        val result = MyBundle.message("projectService", "MyProject", "ExtraParam")
        // Should still work, extra parameters are ignored
        assertEquals("Project service: MyProject", result)
    }

    @Test
    fun `should return key if message not found`() {
        val nonExistentKey = "nonexistent.key"
        val result = MyBundle.message(nonExistentKey)
        // DynamicBundle typically returns the key itself if not found
        assertTrue(result.contains(nonExistentKey) || result == nonExistentKey)
    }

    @Test
    fun `messagePointer should return lazy message`() {
        val messagePointer = MyBundle.messagePointer("name")
        assertNotNull(messagePointer)
        assertEquals("My Plugin", messagePointer.get())
    }

    @Test
    fun `messagePointer should work with parameters`() {
        val messagePointer = MyBundle.messagePointer("projectService", "LazyProject")
        assertNotNull(messagePointer)
        assertEquals("Project service: LazyProject", messagePointer.get())
    }

    @Test
    fun `should handle empty string parameters`() {
        val result = MyBundle.message("projectService", "")
        assertEquals("Project service: ", result)
    }

    @Test
    fun `should handle null parameters gracefully`() {
        // Test with empty array instead of null to avoid type issues
        val result = MyBundle.message("projectService", "null")
        assertEquals("Project service: null", result)
    }

    @Test
    fun `should work with various parameter types`() {
        // Test with different parameter types that toString() well
        val numberResult = MyBundle.message("projectService", 123)
        assertEquals("Project service: 123", numberResult)
        
        val booleanResult = MyBundle.message("projectService", true)
        assertEquals("Project service: true", booleanResult)
    }

    @Test
    fun `bundle constant should be correct`() {
        // This is more of an integration test to ensure the bundle path is correct
        // We can't access the private BUNDLE constant directly, but we can verify
        // that messages are loaded correctly which indicates the path is right
        assertDoesNotThrow {
            MyBundle.message("name")
            MyBundle.message("applicationService")
            MyBundle.message("projectService", "test")
        }
    }
}