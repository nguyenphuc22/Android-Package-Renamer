package com.github.nguyenphuc22.androidpackagerenamer

import com.github.nguyenphuc22.androidpackagerenamer.core.PackageNameValidator
import com.intellij.openapi.ui.InputValidatorEx

class MyValidator : InputValidatorEx {

    val pattern: String get() = PackageNameValidator.PATTERN.pattern

    override fun checkInput(inputString: String?): Boolean {
        return !inputString.isNullOrEmpty() && PackageNameValidator.isValid(inputString)
    }

    override fun canClose(inputString: String?): Boolean {
        return true
    }

    override fun getErrorText(inputString: String?): String? {
        if (inputString != null && !PackageNameValidator.isValid(inputString)) {
            return "Package name is not valid"
        }
        return null
    }
}
