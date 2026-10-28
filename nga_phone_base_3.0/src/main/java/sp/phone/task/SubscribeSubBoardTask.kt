package sp.phone.task

import sp.phone.http.retrofit.NgaApi
import sp.phone.mvp.model.entity.SubBoard

/** 屏蔽或取消屏蔽子版块。 */
class SubscribeSubBoardTask(private val api: () -> NgaApi = NgaApi::get) {

    /** 设置子版块是否显示；成功返回提示，服务端未返回“成功”时以其内容抛出异常。 */
    suspend fun setVisible(subBoard: SubBoard, visible: Boolean): String {
        val response = api().post(getUrl(subBoard, visible))
        check(response.contains("成功")) { response.ifBlank { "操作失败，请稍后重试" } }
        return if (visible) "已取消屏蔽，刷新后生效" else "已屏蔽子版块，刷新后生效"
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
