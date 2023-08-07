package com.github.nguyenphuc22.androidpackagerenamer.objectMain

class InfoProject(newPackageName: String, oldPackageName: String, dataMode: Boolean = false) {
    var packageNameOld : String = oldPackageName
    var packageNameNew : String = newPackageName
    var isDataBindingMode : Boolean = dataMode

}