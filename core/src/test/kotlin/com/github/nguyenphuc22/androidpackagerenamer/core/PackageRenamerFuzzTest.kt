package com.github.nguyenphuc22.androidpackagerenamer.core

import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Property-based/fuzz test: renames many randomly generated Android projects and verifies that
 * no word-boundary reference to the old package is ever left behind, while identifiers that only
 * share the package as a prefix (e.g. `com.acme.appx`) are preserved.
 *
 * Uses a fixed seed so failures are reproducible.
 */
class PackageRenamerFuzzTest {

    @TempDir
    lateinit var tempRoot: Path

    private val random = Random(42)

    private val topSegments = listOf("com", "org", "io", "dev", "acme")
    private val nameSegments = listOf("alpha", "beta", "gamma", "delta", "core", "util", "app", "sample", "testx")

    @Test
    fun `renaming random projects should never leave stale package references`() {
        repeat(25) { iteration ->
            val projectDir = Files.createDirectories(tempRoot.resolve("proj-$iteration"))
            val old = randomPackage()
            val new = randomPackage(old)

            buildProject(projectDir, old)

            PackageRenamer(projectDir).rename(old, new)

            assertNoStaleReferences(projectDir, old, "iteration=$iteration old=$old new=$new")
            assertNewTreeExists(projectDir, new)
        }
    }

    @Test
    fun `renaming random projects with kts should never leave stale references`() {
        repeat(10) { iteration ->
            val projectDir = Files.createDirectories(tempRoot.resolve("kts-$iteration"))
            val old = randomPackage()
            val new = randomPackage(old)

            buildKtsProject(projectDir, old)

            PackageRenamer(projectDir).rename(old, new)

            assertNoStaleReferences(projectDir, old, "kts iteration=$iteration old=$old new=$new")
            assertNewTreeExists(projectDir, new)
        }
    }

    private fun randomPackage(avoid: String? = null): String {
        val candidate = buildString {
            append(topSegments[random.nextInt(topSegments.size)])
            repeat(1 + random.nextInt(3)) { // 2..4 total segments
                append('.').append(nameSegments[random.nextInt(nameSegments.size)])
            }
        }
        if (avoid != null && (candidate == avoid || candidate.startsWith("$avoid."))) {
            return randomPackage(avoid)
        }
        return candidate
    }

    private fun buildProject(projectDir: Path, oldPackage: String) {
        val oldPath = oldPackage.replace('.', '/')
        write(
            projectDir.resolve("app/build.gradle"),
            """
            plugins {
                id 'com.android.application'
            }

            android {
                compileSdk 34
                namespace '$oldPackage'

                defaultConfig {
                    applicationId "$oldPackage"
                }
            }
            """.trimIndent() + "\n",
        )
        write(
            projectDir.resolve("app/src/main/AndroidManifest.xml"),
            """<manifest package="$oldPackage"><application /></manifest>""" + "\n",
        )
        write(
            projectDir.resolve("app/src/main/java/$oldPath/Main.java"),
            """
            package $oldPackage;

            import $oldPackage.util.Helper;

            // sentinel: ${oldPackage}x must never be renamed
            public class Main {
                public static final String NAME = "$oldPackage";
                private Helper helper = new Helper();
            }
            """.trimIndent() + "\n",
        )
        write(
            projectDir.resolve("app/src/main/java/$oldPath/util/Helper.java"),
            """
            package $oldPackage.util;

            public class Helper {
                public static final String TAG = "$oldPackage";
            }
            """.trimIndent() + "\n",
        )
        write(
            projectDir.resolve("app/src/test/java/$oldPath/ExampleTest.java"),
            """
            package $oldPackage;

            public class ExampleTest {
                public void run() { System.out.println("$oldPackage"); }
            }
            """.trimIndent() + "\n",
        )
        write(projectDir.resolve("build/keep.txt"), "dummy\n")
        write(projectDir.resolve("app/build/keep.txt"), "dummy\n")
    }

    private fun buildKtsProject(projectDir: Path, oldPackage: String) {
        val oldPath = oldPackage.replace('.', '/')
        write(
            projectDir.resolve("app/build.gradle.kts"),
            """
            plugins {
                id("com.android.application")
            }

            android {
                compileSdk = 34

                defaultConfig {
                    applicationId = "$oldPackage"
                }
            }
            """.trimIndent() + "\n",
        )
        write(
            projectDir.resolve("app/src/main/AndroidManifest.xml"),
            """<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application /></manifest>""" + "\n",
        )
        write(
            projectDir.resolve("app/src/main/kotlin/$oldPath/Main.kt"),
            """
            package $oldPackage

            // sentinel: ${oldPackage}x must never be renamed
            class Main {
                val name: String = "$oldPackage"
            }
            """.trimIndent() + "\n",
        )
        write(
            projectDir.resolve("app/src/main/kotlin/$oldPath/util/Helper.kt"),
            """
            package $oldPackage.util

            object Helper {
                const val TAG: String = "$oldPackage"
            }
            """.trimIndent() + "\n",
        )
        write(projectDir.resolve("build/keep.txt"), "dummy\n")
        write(projectDir.resolve("app/build/keep.txt"), "dummy\n")
    }

    private fun assertNoStaleReferences(projectDir: Path, oldPackage: String, context: String) {
        val pattern = Regex("\\b" + Regex.escape(oldPackage) + "\\b")
        Files.walk(projectDir).use { stream ->
            stream.filter { Files.isRegularFile(it) }.forEach { file ->
                val content = Files.readString(file)
                assertTrue(!pattern.containsMatchIn(content), "$context: stale reference in $file:\n$content")
            }
        }
    }

    private fun assertNewTreeExists(projectDir: Path, newPackage: String) {
        val newPath = newPackage.replace('.', '/')
        val javaDir = projectDir.resolve("app/src/main/java/$newPath")
        val kotlinDir = projectDir.resolve("app/src/main/kotlin/$newPath")
        assertTrue(Files.isDirectory(javaDir) || Files.isDirectory(kotlinDir))
    }

    private fun write(path: Path, content: String) {
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
    }
}
