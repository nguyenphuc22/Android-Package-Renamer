package com.github.nguyenphuc22.androidpackagerenamer.objectMain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ContentNotificationTest {

    @Test
    fun `should have correct success notification title`() {
        assertEquals("Rename Package Success", ContentNotification.SUCCESS)
    }

    @Test
    fun `should have correct fail notification title`() {
        assertEquals("Rename Package Fail", ContentNotification.FAIL)
    }

    @Test
    fun `should have correct get package name fail notification title`() {
        assertEquals("Get package name fail", ContentNotification.GET_PACKAGE_NAME_FAIL)
    }

    @Test
    fun `should have correct success notification content`() {
        assertEquals("Don't Forget Sync Project with Gradle Files.", ContentNotification.CONTENT_SUCCESS)
    }

    @Test
    fun `should have correct get package name fail notification content`() {
        assertEquals("Package name not found in manifest and gradle.app", ContentNotification.CONTENT_GET_PACKAGE_NAME_FAIL)
    }

    @Test
    fun `all notification constants should be non-null`() {
        assertNotNull(ContentNotification.SUCCESS)
        assertNotNull(ContentNotification.FAIL)
        assertNotNull(ContentNotification.GET_PACKAGE_NAME_FAIL)
        assertNotNull(ContentNotification.CONTENT_SUCCESS)
        assertNotNull(ContentNotification.CONTENT_GET_PACKAGE_NAME_FAIL)
    }

    @Test
    fun `all notification constants should be non-empty`() {
        assertTrue(ContentNotification.SUCCESS.isNotEmpty())
        assertTrue(ContentNotification.FAIL.isNotEmpty())
        assertTrue(ContentNotification.GET_PACKAGE_NAME_FAIL.isNotEmpty())
        assertTrue(ContentNotification.CONTENT_SUCCESS.isNotEmpty())
        assertTrue(ContentNotification.CONTENT_GET_PACKAGE_NAME_FAIL.isNotEmpty())
    }
}