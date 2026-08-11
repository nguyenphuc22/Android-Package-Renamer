package com.github.nguyenphuc22.androidpackagerenamer.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PackageNameExtractorTest {

    @Test
    fun `fromManifest should extract package attribute`() {
        val manifest = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.example.app">
                <application></application>
            </manifest>
        """.trimIndent()
        assertEquals("com.example.app", PackageNameExtractor.fromManifest(manifest))
    }

    @Test
    fun `fromManifest should return null when package attribute is missing or input is null`() {
        val noPackage = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            </manifest>
        """.trimIndent()
        assertNull(PackageNameExtractor.fromManifest(noPackage))
        assertNull(PackageNameExtractor.fromManifest(null))
    }

    @Test
    fun `fromBuildGradle should extract groovy applicationId`() {
        val gradle = """
            android {
                defaultConfig {
                    applicationId "com.example.gradleapp"
                }
            }
        """.trimIndent()
        assertEquals("com.example.gradleapp", PackageNameExtractor.fromBuildGradle(gradle))
    }

    @Test
    fun `fromBuildGradle should extract kts applicationId`() {
        val gradleKts = """
            android {
                defaultConfig {
                    applicationId = "com.example.kotlinapp"
                }
            }
        """.trimIndent()
        assertEquals("com.example.kotlinapp", PackageNameExtractor.fromBuildGradle(gradleKts))
    }

    @Test
    fun `fromBuildGradle should return null when applicationId is missing`() {
        val gradle = """
            android {
                compileSdk 34
            }
        """.trimIndent()
        assertNull(PackageNameExtractor.fromBuildGradle(gradle))
        assertNull(PackageNameExtractor.fromBuildGradle(null))
    }

    @Test
    fun `extract should prioritize manifest, then gradle, then gradle kts`() {
        assertEquals(
            "com.example.manifest",
            PackageNameExtractor.extract(
                """<manifest package="com.example.manifest"></manifest>""",
                """applicationId "com.example.gradle"""",
                """applicationId = "com.example.kts"""",
            ),
        )
        assertEquals(
            "com.example.gradle",
            PackageNameExtractor.extract(null, """applicationId "com.example.gradle"""", """applicationId = "com.example.kts""""),
        )
        assertEquals(
            "com.example.kts",
            PackageNameExtractor.extract(null, null, """applicationId = "com.example.kts""""),
        )
        assertNull(PackageNameExtractor.extract(null, null, null))
    }
}
