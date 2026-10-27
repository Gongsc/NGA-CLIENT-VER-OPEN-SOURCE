package sp.phone.task

import androidx.lifecycle.LifecycleOwner
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.coroutines.scopeOf
import gov.anzong.androidnga.http.OnHttpCallBack
import sp.phone.http.retrofit.NgaApi
import sp.phone.mvp.model.entity.SubBoard

/** 屏蔽或取消屏蔽子版块，随页面销毁取消。 */
class SubscribeSubBoardTask @JvmOverloads constructor(
    private val owner: LifecycleOwner?,
    private val api: () -> NgaApi = NgaApi::get,
) {

    fun subscribe(subBoard: SubBoard, callBack: OnHttpCallBack<String>) =
        post(getUrl(subBoard, true), "取消屏蔽成功，请刷新界面！", callBack)

    fun unsubscribe(subBoard: SubBoard, callBack: OnHttpCallBack<String>) =
        post(getUrl(subBoard, false), "屏蔽子板块成功，请刷新界面！", callBack)

    private fun post(url: String, success: String, callBack: OnHttpCallBack<String>) {
        scopeOf(owner).launchWithCallback({ s: String ->
            if (s.contains("成功")) callBack.onSuccess(success) else callBack.onError(s)
        }, { callBack.onError(it.message) }) { api().post(url) }
    }

    internal fun getUrl(subBoard: SubBoard, isSubscribe: Boolean): String {
        val type = subBoard.type
        val action = getAction(type, isSubscribe)
        val fid = if (type == 1) subBoard.tidStr else subBoard.fid.toString()
        return String.format(
            "http://bbs.ngacn.cc/nuke.php?__lib=user_option&__act=set&raw=3&type=%s&__output=8&fid=%s&%s=%s",
            type, subBoard.parentFidStr, action, fid,
        )
    }

    // NGA 后台好变态啊，某个板块的操作居然是反的 比如 “优惠信息 购物指南”
    private fun getAction(type: Int, isSubscribe: Boolean): String =
        if (type == 1) (if (isSubscribe) "del" else "add") else (if (isSubscribe) "add" else "del")
}
