package com.github.nguyenphuc22.androidpackagerenamer.core

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Shared test helper that copies an Android project fixture from the test resources
 * into a temporary directory.
 */
object TestProjectHelper {

    fun copyFixture(name: String, target: Path) {
        val url = requireNotNull(javaClass.classLoader.getResource("fixtures/$name")) { "fixture not found: $name" }
        val source = Paths.get(url.toURI())
        Files.walk(source).use { stream ->
            stream.forEach { path ->
                val dest = target.resolve(source.relativize(path).toString())
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest)
                } else {
                    Files.createDirectories(dest.parent)
                    Files.copy(path, dest)
                }
            }
        }
    }
}
