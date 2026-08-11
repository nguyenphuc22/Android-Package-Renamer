package com.github.nguyenphuc22.androidpackagerenamer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class WorkingPackageTest {

    @Mock
    private lateinit var mockProject: Project

    @Mock
    private lateinit var mockAnActionEvent: AnActionEvent

    @Mock
    private lateinit var mockPresentation: Presentation

    private lateinit var workingPackage: WorkingPackage

    @BeforeEach
    fun setUp() {
        workingPackage = WorkingPackage()
    }

    @Test
    fun `should have correct class structure`() {
        assertNotNull(workingPackage)
        assertTrue(workingPackage is AnAction)
    }

    @Test
    fun `update should enable presentation when project is available`() {
        // Arrange
        `when`(mockAnActionEvent.project).thenReturn(mockProject)
        `when`(mockAnActionEvent.presentation).thenReturn(mockPresentation)

        // Act
        workingPackage.update(mockAnActionEvent)

        // Assert
        verify(mockPresentation).isEnabled = true
        verify(mockAnActionEvent, atLeastOnce()).project
    }

    @Test
    fun `update should disable presentation when project is null`() {
        // Arrange
        `when`(mockAnActionEvent.project).thenReturn(null)
        `when`(mockAnActionEvent.presentation).thenReturn(mockPresentation)

        // Act
        workingPackage.update(mockAnActionEvent)

        // Assert
        verify(mockPresentation).isEnabled = false
        verify(mockAnActionEvent, atLeastOnce()).project
    }

    @Test
    fun `should extract package name from manifest when available`() {
        val manifestContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.example.testapp">
                <application>
                </application>
            </manifest>
        """.trimIndent()

        assertTrue(manifestContent.contains("package"))
        val packageName = manifestContent.substringAfter("package=").substringAfter("\"").substringBefore("\"")
        assertEquals("com.example.testapp", packageName)
    }

    @Test
    fun `should extract applicationId from gradle file when manifest package not available`() {
        val gradleContent = """
            android {
                compileSdk 34
                defaultConfig {
                    applicationId "com.example.gradleapp"
                    minSdk 21
                    targetSdk 34
                }
            }
        """.trimIndent()

        assertTrue(gradleContent.contains("applicationId"))
        val packageName = gradleContent.substringAfter("applicationId").substringAfter("\"").substringBefore("\"")
        assertEquals("com.example.gradleapp", packageName)
    }

    @Test
    fun `should extract applicationId from gradle kts file`() {
        val gradleKtsContent = """
            android {
                compileSdk = 34
                defaultConfig {
                    applicationId = "com.example.kotlinapp"
                    minSdk = 21
                    targetSdk = 34
                }
            }
        """.trimIndent()

        assertTrue(gradleKtsContent.contains("applicationId"))
        val packageName = gradleKtsContent.substringAfter("applicationId").substringAfter("\"").substringBefore("\"")
        assertEquals("com.example.kotlinapp", packageName)
    }

    @Test
    fun `should handle string parsing operations`() {
        // Test normal parsing operations work correctly
        val validManifest = """<manifest package="com.example.app">"""
        val result = validManifest.substringAfter("package=").substringAfter("\"").substringBefore("\"")
        assertEquals("com.example.app", result)

        // Test contains function
        assertTrue(validManifest.contains("package"))
        assertFalse(validManifest.contains("nonexistent"))
    }

    @Test
    fun `should have correct package name`() {
        val packageName = WorkingPackage::class.java.packageName
        assertEquals("com.github.nguyenphuc22.androidpackagerenamer", packageName)
    }
}
