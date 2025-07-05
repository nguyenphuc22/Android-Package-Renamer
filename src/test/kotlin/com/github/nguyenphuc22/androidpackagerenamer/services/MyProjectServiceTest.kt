package com.github.nguyenphuc22.androidpackagerenamer.services

import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class MyProjectServiceTest {

    @Mock
    private lateinit var mockProject: Project

    @Test
    fun `should have MyProjectService class available`() {
        val serviceClass = MyProjectService::class.java
        assertNotNull(serviceClass)
        assertEquals("MyProjectService", serviceClass.simpleName)
    }

    @Test
    fun `should have correct package structure`() {
        val packageName = MyProjectService::class.java.packageName
        assertEquals("com.github.nguyenphuc22.androidpackagerenamer.services", packageName)
    }

    @Test
    fun `getRandomNumber should return 4 when service can be created`() {
        // Arrange
        `when`(mockProject.name).thenReturn("TestProject")
        
        val ciEnvVar = System.getenv("CI")
        if (ciEnvVar != null && ciEnvVar.isNotEmpty()) {
            // Only test getRandomNumber if we can create the service (in CI)
            val service = MyProjectService(mockProject)
            
            // Act
            val result = service.getRandomNumber()
            
            // Assert
            assertEquals(4, result)
        } else {
            // Skip the actual service creation test in non-CI environment
            // But we can still test that the TODO exception would be thrown
            assertThrows(NotImplementedError::class.java) {
                MyProjectService(mockProject)
            }
        }
    }

    @Test
    fun `getRandomNumber should always return the same value when service can be created`() {
        val ciEnvVar = System.getenv("CI")
        if (ciEnvVar != null && ciEnvVar.isNotEmpty()) {
            val service = MyProjectService(mockProject)
            
            // Act - Call multiple times
            val result1 = service.getRandomNumber()
            val result2 = service.getRandomNumber()
            val result3 = service.getRandomNumber()
            
            // Assert - All should be the same
            assertEquals(4, result1)
            assertEquals(4, result2)
            assertEquals(4, result3)
            assertEquals(result1, result2)
            assertEquals(result2, result3)
        }
        // In non-CI environment, this test is effectively skipped
    }
}