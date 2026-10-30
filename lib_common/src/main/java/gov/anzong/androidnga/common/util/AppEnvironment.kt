package gov.anzong.androidnga.common.util

import android.os.Environment

object AppEnvironment {

    @JvmStatic
    val externalStoragePictureDirectory: String
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath

    @JvmStatic
    val externalStorageDirectory: String
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
}
