package com.github.nguyenphuc22.androidpackagerenamer.core.cli

import com.github.nguyenphuc22.androidpackagerenamer.core.PackageRenamer
import com.github.nguyenphuc22.androidpackagerenamer.core.RenameOptions
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.system.exitProcess

private data class CliOptions(
    val projectDir: Path,
    val newPackage: String,
    val oldPackage: String? = null,
    val cleanBuildDirectories: Boolean = true,
    val dryRun: Boolean = false,
)

fun main(args: Array<String>) {
    exitProcess(runCli(args, System.out, System.err))
}

/**
 * Testable entry point. Returns the process exit code and writes output to the provided streams.
 */
fun runCli(args: Array<String>, out: PrintStream, err: PrintStream): Int {
    if (args.contains("--help") || args.contains("-h")) {
        printUsage(out)
        return 0
    }

    val options = parseArgs(args)
    if (options == null) {
        printUsage(err)
        return 2
    }

    return try {
        if (options.dryRun) {
            dryRun(options, out)
        } else {
            runRename(options, out)
        }
        0
    } catch (e: Exception) {
        err.println("Error: ${e.message}")
        1
    }
}

private fun runRename(options: CliOptions, out: PrintStream) {
    if (!Files.isDirectory(options.projectDir)) {
        throw IllegalStateException("Project directory does not exist: ${options.projectDir}")
    }
    val renamer = PackageRenamer(options.projectDir, RenameOptions(cleanBuildDirectories = options.cleanBuildDirectories))
    val current = renamer.currentPackageName()
        ?: throw IllegalStateException("Cannot determine the current package name from the Android project")
    val old = options.oldPackage ?: current
    out.println("Current package: $current")
    val result = renamer.rename(old, options.newPackage)
    out.println(
        "Renamed '$old' to '${options.newPackage}': ${result.movedFiles} files moved, " +
            "${result.updatedFiles} files updated, ${result.cleanedBuildDirectories} build directories cleaned.",
    )
}

private fun dryRun(options: CliOptions, out: PrintStream) {
    if (!Files.isDirectory(options.projectDir)) {
        throw IllegalStateException("Project directory does not exist: ${options.projectDir}")
    }
    val tempDir = Files.createTempDirectory("package-renamer-dry-run")
    try {
        copyProject(options.projectDir, tempDir)
        val renamer = PackageRenamer(tempDir, RenameOptions(cleanBuildDirectories = options.cleanBuildDirectories))
        val current = renamer.currentPackageName()
            ?: throw IllegalStateException("Cannot determine the current package name from the Android project")
        val old = options.oldPackage ?: current
        val result = renamer.rename(old, options.newPackage)
        out.println(
            "Dry run: would rename '$old' to '${options.newPackage}': ${result.movedFiles} files moved, " +
                "${result.updatedFiles} files updated.",
        )
    } finally {
        deleteRecursively(tempDir)
    }
}

private fun copyProject(source: Path, target: Path) {
    Files.walk(source).use { stream ->
        stream.forEach { path ->
            val relative = source.relativize(path)
            if (relative.nameCount > 0 && relative.getName(0).toString() in EXCLUDED_DIRECTORIES) return@forEach
            val dest = target.resolve(relative)
            if (Files.isDirectory(path)) {
                Files.createDirectories(dest)
            } else {
                Files.copy(path, dest)
            }
        }
    }
}

private fun deleteRecursively(dir: Path) {
    if (!Files.exists(dir)) return
    Files.walk(dir).use { stream ->
        stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
}

private fun parseArgs(args: Array<String>): CliOptions? {
    var projectDir: Path? = null
    var oldPackage: String? = null
    var newPackage: String? = null
    var clean = true
    var dryRun = false
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--project-dir" -> projectDir = Path.of(args.getOrNull(++i) ?: return null)
            "--old-package" -> oldPackage = args.getOrNull(++i) ?: return null
            "--new-package" -> newPackage = args.getOrNull(++i) ?: return null
            "--no-clean" -> clean = false
            "--dry-run" -> dryRun = true
            else -> return null
        }
        i++
    }
    val dir = projectDir ?: return null
    val newName = newPackage ?: return null
    return CliOptions(dir, newName, oldPackage, clean, dryRun)
}

private fun printUsage(out: PrintStream) {
    out.println(
        """
        Usage: package-renamer-core --project-dir <path> --new-package <name> [options]

        Renames the application package of an Android project (single 'app' module).

        Options:
          --project-dir <path>     Root directory of the Android project (required)
          --new-package <name>     New package name (required)
          --old-package <name>     Old package name. When omitted, it is detected
                                   automatically from AndroidManifest.xml / build.gradle(.kts)
          --no-clean               Do not delete the build directories after renaming
          --dry-run                Preview the changes on a temporary copy without touching the project
          -h, --help               Show this help
        """.trimIndent(),
    )
}

private val EXCLUDED_DIRECTORIES = listOf("build", ".gradle", ".idea")
