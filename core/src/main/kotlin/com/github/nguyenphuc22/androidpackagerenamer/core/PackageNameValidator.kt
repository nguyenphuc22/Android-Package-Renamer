package com.github.nguyenphuc22.androidpackagerenamer.core

/**
 * Validates Android package names (e.g. `com.example.myapp`).
 *
 * Pure logic extracted from the IntelliJ plugin's `MyValidator` and `ManagerFile.validateNewPackageName`.
 */
object PackageNameValidator {

    val PATTERN = Regex("^([A-Za-z]{1}[A-Za-z\\d_]*\\.)+[A-Za-z][A-Za-z\\d_]*\$")

    fun isValid(name: String): Boolean =
        name.isNotEmpty() && name.isNotBlank() && name.matches(PATTERN)
}
