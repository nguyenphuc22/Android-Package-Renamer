package com.github.nguyenphuc22.androidpackagerenamer.services

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MyApplicationServiceTest {

    @Test
    fun `should have MyApplicationService class available`() {
        // Simple test to verify the class exists and can be referenced
        val serviceClass = MyApplicationService::class.java
        assertNotNull(serviceClass)
        assertEquals("MyApplicationService", serviceClass.simpleName)
    }

    @Test
    fun `should be able to create instance when CI env var is set`() {
        // This test only runs if CI env var is already set
        val ciEnvVar = System.getenv("CI")
        if (ciEnvVar != null && ciEnvVar.isNotEmpty()) {
            assertDoesNotThrow {
                MyApplicationService()
            }
        } else {
            // If not in CI, expect TODO exception
            assertThrows(NotImplementedError::class.java) {
                MyApplicationService()
            }
        }
    }

    @Test
    fun `should have correct package structure`() {
        val packageName = MyApplicationService::class.java.packageName
        assertEquals("com.github.nguyenphuc22.androidpackagerenamer.services", packageName)
    }
}