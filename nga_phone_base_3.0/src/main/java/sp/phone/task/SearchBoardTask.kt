package sp.phone.task

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import sp.phone.http.OnSimpleHttpCallBack
import sp.phone.http.retrofit.NgaApi
import sp.phone.mvp.model.BoardModel
import sp.phone.mvp.model.entity.Board
import sp.phone.util.StringUtils

/** 按名称搜索版块；未找到或请求失败时回调 null。 */
object SearchBoardTask {

    @JvmStatic
    fun execute(boardName: String, callBack: OnSimpleHttpCallBack<Board?>) {
        AppScope.launchWithCallback({ board: Board? -> callBack.onResult(board) }, { callBack.onResult(null) }) {
            val response = NgaApi.get().get("http://bbs.nga.cn/forum.php?&__output=8&key=" + StringUtils.encodeUrl(boardName, "gbk"))
            try {
                val obj = NgaJson.parseObject(response).getJSONObject("data").getJSONObject("0")
                val fid = obj.getInteger("fid")
                BoardModel.getInstance().findBoard(fid.toString()) ?: Board(fid, obj.getString("name"))
            } catch (e: Exception) {
                null
            }
        }
    }
}
