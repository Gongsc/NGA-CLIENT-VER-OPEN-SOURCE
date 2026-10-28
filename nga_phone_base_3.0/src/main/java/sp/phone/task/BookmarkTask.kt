package sp.phone.task

import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import sp.phone.http.retrofit.NgaApi
import sp.phone.util.NLog
import sp.phone.util.StringUtils

/** 收藏主题或回复；成功时提示服务端返回的文字，失败只记录日志。 */
object BookmarkTask {

    private val url = Utils.getNGAHost() + "nuke.php?__lib=topic_favor&lite=js&noprefix&__act=topic_favor&action=add&tid="

    @JvmStatic
    fun execute(tid: Int) = post(url + tid)

    @JvmStatic
    fun execute(tid: String, pid: String) = post("$url$tid&pid=$pid")

    private fun post(postUrl: String) {
        AppScope.launchWithCallback({ msg: String? ->
            if (!StringUtils.isEmpty(msg)) ToastUtils.info(msg!!.trim())
        }, { NLog.e(it.message) }) {
            val result = NgaApi.get().post(postUrl)
            StringUtils.getStringBetween(result, 0, "{\"0\":\"", "\"},\"time\"").result
        }
    }
}
