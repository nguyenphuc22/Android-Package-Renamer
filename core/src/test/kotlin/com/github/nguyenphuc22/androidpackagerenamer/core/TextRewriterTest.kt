package com.github.nguyenphuc22.androidpackagerenamer.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TextRewriterTest {

    private val manifestWithPackage = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="com.example.old">
            <application></application>
        </manifest>
    """.trimIndent()

    // ---------- replacePackageReferences ----------

    @Test
    fun `replacePackageReferences should replace all occurrences`() {
        val content = "package com.example.old\nimport com.example.old.util.Helper\nval x = \"com.example.old\""
        val updated = TextRewriter.replacePackageReferences(content, "com.example.old", "com.example.new")
        assertTrue(!updated.contains("com.example.old"))
        assertTrue(updated.contains("com.example.new"))
    }

    @Test
    fun `replacePackageReferences should respect word boundaries`() {
        val content = "com.foo.app com.foo.appx mycom.foo.app com.foo.app.core comfoo.app"
        val updated = TextRewriter.replacePackageReferences(content, "com.foo.app", "com.foo.app2")
        assertEquals("com.foo.app2 com.foo.appx mycom.foo.app com.foo.app2.core comfoo.app", updated)
    }

    // ---------- updateManifestPackage ----------

    @Test
    fun `updateManifestPackage should replace existing package attribute`() {
        val updated = TextRewriter.updateManifestPackage(manifestWithPackage, "com.example.new")
        assertTrue(updated.contains("package=\"com.example.new\""))
        assertTrue(!updated.contains("com.example.old"))
    }

    @Test
    fun `updateManifestPackage should leave manifest unchanged when package attribute is missing`() {
        val noPackage = """<manifest xmlns:android="http://schemas.android.com/apk/res/android">"""
        assertEquals(noPackage, TextRewriter.updateManifestPackage(noPackage, "com.example.new"))
    }

    @Test
    fun `updateManifestPackage should throw on empty package attribute`() {
        assertThrows(IllegalStateException::class.java) {
            TextRewriter.updateManifestPackage("""<manifest package=""></manifest>""", "com.example.new")
        }
    }

    // ---------- updateApplicationId ----------

    @Test
    fun `updateApplicationId should replace groovy and kts applicationId`() {
        val groovy = """applicationId "com.example.old" """
        assertEquals("""applicationId "com.example.new" """, TextRewriter.updateApplicationId(groovy, "com.example.new"))

        val kts = """applicationId = "com.example.old" """
        assertEquals("""applicationId = "com.example.new" """, TextRewriter.updateApplicationId(kts, "com.example.new"))
    }

    @Test
    fun `updateApplicationId should not touch applicationIdSuffix`() {
        val content = "applicationIdSuffix \".debug\"\n    applicationId \"com.example.old\""
        val updated = TextRewriter.updateApplicationId(content, "com.example.new")
        assertTrue(updated.contains("applicationIdSuffix \".debug\""))
        assertTrue(updated.contains("applicationId \"com.example.new\""))
    }

    @Test
    fun `updateApplicationId should leave content untouched when applicationId is missing`() {
        val content = "android { compileSdk 34 }"
        assertEquals(content, TextRewriter.updateApplicationId(content, "com.example.new"))
    }

    // ---------- addNamespaceIfMissing ----------

    @Test
    fun `addNamespaceIfMissing should insert groovy and kts namespace`() {
        val groovy = "android {\n    compileSdk 34\n}"
        val updatedGroovy = TextRewriter.addNamespaceIfMissing(groovy, "com.example.new", isKotlinDsl = false)
        assertTrue(updatedGroovy.contains("namespace 'com.example.new'"))

        val kts = "android {\n    compileSdk = 34\n}"
        val updatedKts = TextRewriter.addNamespaceIfMissing(kts, "com.example.new", isKotlinDsl = true)
        assertTrue(updatedKts.contains("namespace = \"com.example.new\""))
    }

    @Test
    fun `addNamespaceIfMissing should not duplicate when namespace is present`() {
        val groovy = "android {\n    namespace 'com.example.old'\n}"
        assertEquals(groovy, TextRewriter.addNamespaceIfMissing(groovy, "com.example.new", isKotlinDsl = false))
    }

    // ---------- updateNamespace ----------

    @Test
    fun `updateNamespace should update existing groovy single-quote namespace`() {
        val groovy = "android {\n    namespace 'com.example.old'\n}"
        val updated = TextRewriter.updateNamespace(groovy, "com.example.new", isKotlinDsl = false)
        assertTrue(updated.contains("namespace 'com.example.new'"))
        assertTrue(!updated.contains("com.example.old"))
    }

    @Test
    fun `updateNamespace should update existing groovy double-quote namespace`() {
        val groovy = "android {\n    namespace \"com.example.old\"\n}"
        val updated = TextRewriter.updateNamespace(groovy, "com.example.new", isKotlinDsl = false)
        assertTrue(updated.contains("namespace 'com.example.new'"))
        assertTrue(!updated.contains("com.example.old"))
    }

    @Test
    fun `updateNamespace should update existing kts namespace`() {
        val kts = "android {\n    namespace = \"com.example.old\"\n}"
        val updated = TextRewriter.updateNamespace(kts, "com.example.new", isKotlinDsl = true)
        assertTrue(updated.contains("namespace = \"com.example.new\""))
        assertTrue(!updated.contains("com.example.old"))
    }

    @Test
    fun `updateNamespace should insert when missing`() {
        val groovy = "android {\n    compileSdk 34\n}"
        val updated = TextRewriter.updateNamespace(groovy, "com.example.new", isKotlinDsl = false)
        assertTrue(updated.contains("namespace 'com.example.new'"))
    }
}
