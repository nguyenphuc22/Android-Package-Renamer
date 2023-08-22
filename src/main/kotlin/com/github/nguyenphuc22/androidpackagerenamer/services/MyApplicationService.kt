package com.github.nguyenphuc22.androidpackagerenamer.services

import com.github.nguyenphuc22.androidpackagerenamer.MyBundle

class MyApplicationService {

    init {
        println(MyBundle.message("applicationService"))

        System.getenv("CI")
            ?: TODO("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }
}
