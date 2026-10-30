package gov.anzong.androidnga.debug

import android.os.Environment
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.common.util.FileUtils
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * 调试模式下把请求与响应追加写入下载目录，便于排查接口问题。
 * @author Justwen
 */
object Debugger {

    private var debugMode = false

    @JvmStatic
    fun collectBody(body: String) = collect("body.json", body)

    @JvmStatic
    fun collectRequest(request: Request) = collect("request.json", request.toString())

    private fun collect(name: String, content: String) {
        if (!debugMode) {
            return
        }
        try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            FileUtils.write(File(dir, "gov.anzong.androidnga/debug/$name"), content + "\n\n", true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun toggleDebugMode() {
        debugMode = !debugMode
        ToastUtils.show(if (debugMode) "调试模式开启" else "调试模式关闭")
    }
}
