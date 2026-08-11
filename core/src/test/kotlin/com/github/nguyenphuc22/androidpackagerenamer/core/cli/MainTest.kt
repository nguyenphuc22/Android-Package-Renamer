package com.github.nguyenphuc22.androidpackagerenamer.core.cli

import com.github.nguyenphuc22.androidpackagerenamer.core.TestProjectHelper
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MainTest {

    @TempDir
    lateinit var tempDir: Path

    private fun runCli(args: Array<String>): Pair<Int, String> {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val code = runCli(args, PrintStream(out), PrintStream(err))
        return code to (out.toString() + err.toString())
    }

    @Test
    fun `should rename a real project and print a summary`() {
        TestProjectHelper.copyFixture("manifest-groovy-java", tempDir)
        val (code, output) = runCli(
            arrayOf("--project-dir", tempDir.toString(), "--new-package", "com.example.newname"),
        )

        assertEquals(0, code)
        assertTrue(output.contains("Current package: com.example.oldname"))
        assertTrue(output.contains("files moved"))
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/main/java/com/example/newname/MainActivity.java")))
        assertFalse(Files.exists(tempDir.resolve("app/src/main/java/com/example/oldname")))
    }

    @Test
    fun `should use explicit old package when provided`() {
        TestProjectHelper.copyFixture("kts-kotlin-no-namespace", tempDir)
        val (code, _) = runCli(
            arrayOf(
                "--project-dir", tempDir.toString(),
                "--old-package", "com.example.oldname",
                "--new-package", "com.example.newname",
                "--no-clean",
            ),
        )

        assertEquals(0, code)
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/main/kotlin/com/example/newname/MainActivity.kt")))
    }

    @Test
    fun `dry run should preview without modifying the project`() {
        TestProjectHelper.copyFixture("manifest-groovy-java", tempDir)
        val (code, output) = runCli(
            arrayOf(
                "--project-dir", tempDir.toString(),
                "--new-package", "com.example.newname",
                "--dry-run",
            ),
        )

        assertEquals(0, code)
        assertTrue(output.contains("Dry run"))
        // Project unchanged
        assertTrue(Files.isRegularFile(tempDir.resolve("app/src/main/java/com/example/oldname/MainActivity.java")))
        assertFalse(Files.exists(tempDir.resolve("app/src/main/java/com/example/newname")))
    }

    @Test
    fun `should print usage and return 2 when arguments are missing`() {
        val (code, output) = runCli(emptyArray())
        assertEquals(2, code)
        assertTrue(output.contains("Usage:"))
    }

    @Test
    fun `should print usage and return 0 for help`() {
        val (code, output) = runCli(arrayOf("--help"))
        assertEquals(0, code)
        assertTrue(output.contains("Usage:"))
    }

    @Test
    fun `should return 1 for non-android project`() {
        val (code, output) = runCli(
            arrayOf("--project-dir", tempDir.toString(), "--new-package", "com.example.newname"),
        )
        assertEquals(1, code)
        assertTrue(output.contains("Cannot determine the current package name"))
    }
}
