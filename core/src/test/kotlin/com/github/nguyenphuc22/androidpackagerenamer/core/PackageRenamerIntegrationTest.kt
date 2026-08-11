package com.github.nguyenphuc22.androidpackagerenamer.core

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PackageRenamerIntegrationTest {

    @TempDir
    lateinit var tempDir: Path

    // ---------- manifest + groovy + java fixture ----------

    @Test
    fun `should detect current package from manifest`() {
        TestProjectHelper.copyFixture("manifest-groovy-java", tempDir)
        assertEquals("com.example.oldname", PackageRenamer(tempDir).currentPackageName())
    }

    @Test
    fun `should rename manifest-groovy-java project end to end`() {
        TestProjectHelper.copyFixture("manifest-groovy-java", tempDir)
        val renamer = PackageRenamer(tempDir)

        val result = renamer.rename("com.example.oldname", "com.example.newname")

        assertTrue(result.movedFiles > 0, "files should be moved")
        assertTrue(result.updatedFiles > 0, "files should be updated")
        assertEquals(3, result.cleanedBuildDirectories)

        // Directories moved and old package removed
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/main/java/com/example/newname/MainActivity.java")))
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/main/java/com/example/newname/util/Helper.java")))
        assertFalse(Files.exists(tempDir.resolve("app/src/main/java/com/example/oldname")))

        // Source contents rewritten
        val mainActivity = read(tempDir.resolve("app/src/main/java/com/example/newname/MainActivity.java"))
        assertTrue(mainActivity.contains("package com.example.newname;"))
        assertTrue(mainActivity.contains("import com.example.newname.util.Helper;"))

        val helper = read(tempDir.resolve("app/src/main/java/com/example/newname/util/Helper.java"))
        assertTrue(helper.contains("package com.example.newname.util;"))
        assertTrue(helper.contains("com.example.newname"))
        assertFalse(helper.contains("com.example.oldname"))

        // Test and androidTest source sets moved
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/test/java/com/example/newname/ExampleUnitTest.java")))
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/androidTest/java/com/example/newname/ExampleInstrumentedTest.java")))
        assertTrue(read(tempDir.resolve("app/src/test/java/com/example/newname/ExampleUnitTest.java")).contains("package com.example.newname;"))

        // Manifest updated
        assertTrue(read(tempDir.resolve("app/src/main/AndroidManifest.xml")).contains("package=\"com.example.newname\""))

        // Build.gradle updated (applicationId + namespace)
        val buildGradle = read(tempDir.resolve("app/build.gradle"))
        assertTrue(buildGradle.contains("applicationId \"com.example.newname\""))
        assertTrue(buildGradle.contains("namespace 'com.example.newname'"))

        // Unrelated resource file untouched (no package reference)
        assertTrue(read(tempDir.resolve("app/src/main/res/layout/activity_main.xml")).contains("android:layout_width=\"match_parent\""))

        // Build directories cleaned
        assertFalse(Files.exists(tempDir.resolve("build")))
        assertFalse(Files.exists(tempDir.resolve("app/build")))
        assertFalse(Files.exists(tempDir.resolve(".gradle")))

        // New package is now detected
        assertEquals("com.example.newname", renamer.currentPackageName())
    }

    @Test
    fun `should clean up the whole old package directory tree when the root package changes`() {
        TestProjectHelper.copyFixture("manifest-groovy-java", tempDir)
        PackageRenamer(tempDir).rename("com.example.oldname", "org.acme.newname")

        // New tree present
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/main/java/org/acme/newname/MainActivity.java")))
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/test/java/org/acme/newname/ExampleUnitTest.java")))

        // Old tree fully removed (including the now-empty com/example parents)
        assertFalse(Files.exists(tempDir.resolve("app/src/main/java/com")))
        assertFalse(Files.exists(tempDir.resolve("app/src/test/java/com")))
        assertFalse(Files.exists(tempDir.resolve("app/src/androidTest/java/com")))
    }

    @Test
    fun `should update only android files without touching sources`() {
        TestProjectHelper.copyFixture("manifest-groovy-java", tempDir)
        PackageRenamer(tempDir).updateAndroidFilesOnly("com.example.newname")

        // Sources NOT moved or rewritten
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/main/java/com/example/oldname/MainActivity.java")))
        assertTrue(read(tempDir.resolve("app/src/main/java/com/example/oldname/MainActivity.java")).contains("package com.example.oldname;"))

        // Android files updated
        assertTrue(read(tempDir.resolve("app/src/main/AndroidManifest.xml")).contains("package=\"com.example.newname\""))
        assertTrue(read(tempDir.resolve("app/build.gradle")).contains("applicationId \"com.example.newname\""))
        assertTrue(read(tempDir.resolve("app/build.gradle")).contains("namespace 'com.example.newname'"))

        // Build directories cleaned
        assertFalse(Files.exists(tempDir.resolve("build")))
        assertFalse(Files.exists(tempDir.resolve("app/build")))
        assertFalse(Files.exists(tempDir.resolve(".gradle")))
    }

    // ---------- kts + kotlin fixture (no namespace) ----------

    @Test
    fun `should detect current package from gradle kts applicationId`() {
        TestProjectHelper.copyFixture("kts-kotlin-no-namespace", tempDir)
        assertEquals("com.example.oldname", PackageRenamer(tempDir).currentPackageName())
    }

    @Test
    fun `should rename kts-kotlin project and insert namespace`() {
        TestProjectHelper.copyFixture("kts-kotlin-no-namespace", tempDir)
        val renamer = PackageRenamer(tempDir, RenameOptions(cleanBuildDirectories = false))

        val result = renamer.rename("com.example.oldname", "com.example.newname")

        assertTrue(result.movedFiles > 0)
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/main/kotlin/com/example/newname/MainActivity.kt")))
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/main/kotlin/com/example/newname/util/Helper.kt")))
        assertFalse(Files.exists(tempDir.resolve("app/src/main/kotlin/com/example/oldname")))

        assertTrue(read(tempDir.resolve("app/src/main/kotlin/com/example/newname/MainActivity.kt")).contains("package com.example.newname"))

        val buildGradleKts = read(tempDir.resolve("app/build.gradle.kts"))
        assertTrue(buildGradleKts.contains("applicationId = \"com.example.newname\""))
        assertTrue(buildGradleKts.contains("namespace = \"com.example.newname\""))

        // Manifest had no package attribute -> the renamer inserts the new package attribute
        assertTrue(read(tempDir.resolve("app/src/main/AndroidManifest.xml")).contains("package=\"com.example.newname\""))

        // With cleanBuildDirectories = false, build dirs (if any) are kept - fixture has none
        assertEquals(0, result.cleanedBuildDirectories)
    }

    // ---------- databinding fixture ----------

    @Test
    fun `should rewrite databinding layout package references`() {
        TestProjectHelper.copyFixture("databinding", tempDir)
        PackageRenamer(tempDir).rename("com.example.oldname", "com.example.newname")

        val layout = read(tempDir.resolve("app/src/main/res/layout/activity_main.xml"))
        assertTrue(layout.contains("com.example.newname.MainViewModel"))
        assertFalse(layout.contains("com.example.oldname"))

        assertTrue(read(tempDir.resolve("app/src/main/java/com/example/newname/MainViewModel.java")).contains("package com.example.newname;"))
        assertTrue(read(tempDir.resolve("app/src/main/java/com/example/newname/MainViewModel.java")).contains("com.example.newname"))
    }

    // ---------- validation ----------

    @Test
    fun `rename should reject invalid input`() {
        TestProjectHelper.copyFixture("manifest-groovy-java", tempDir)
        val renamer = PackageRenamer(tempDir)

        assertThrows(IllegalArgumentException::class.java) { renamer.rename("", "com.example.new") }
        assertThrows(IllegalArgumentException::class.java) { renamer.rename("com.example.old", "  ") }
        assertThrows(IllegalArgumentException::class.java) { renamer.rename("com.example.old", "com.example.old") }
        assertThrows(IllegalArgumentException::class.java) { renamer.rename("com.example.old", "invalid_package") }
    }

    @Test
    fun `currentPackageName should return null for non-android project`() {
        assertEquals(null, PackageRenamer(tempDir).currentPackageName())
    }

    // ---------- helpers ----------

    private fun read(path: Path): String = Files.readString(path)
}
