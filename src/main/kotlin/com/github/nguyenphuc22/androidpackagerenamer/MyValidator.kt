package com.github.nguyenphuc22.androidpackagerenamer

import com.intellij.openapi.ui.InputValidatorEx

class MyValidator : InputValidatorEx {
    val pattern = "^([A-Za-z]{1}[A-Za-z\\d_]*\\.)+[A-Za-z][A-Za-z\\d_]*\$"
    override fun checkInput(inputString: String?): Boolean {
        return !inputString.isNullOrEmpty() && inputString.isNotBlank() && inputString.matches(Regex(pattern))
    }

    override fun canClose(inputString: String?): Boolean {
        return true
    }
    override fun getErrorText(inputString: String?): String? {
        var result : String? = null
        if (inputString != null) {
            if (!inputString.matches(Regex(pattern)))
                result = "Package name is not valid"
        }
        return result
    }
}