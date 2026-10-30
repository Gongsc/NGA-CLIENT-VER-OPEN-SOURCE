package gov.anzong.androidnga.task

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.network.OnSimpleHttpCallBack
import gov.anzong.androidnga.network.retrofit.NgaApi
import gov.anzong.androidnga.mvvm.model.BoardModel
import gov.anzong.androidnga.mvvm.model.entity.Board
import gov.anzong.androidnga.util.StringUtils

/** 按名称搜索版块；未找到或请求失败时回调 null。 */
object SearchBoardTask {

    @JvmStatic
    fun execute(boardName: String, callBack: OnSimpleHttpCallBack<Board?>) {
        AppScope.launchWithCallback({ board: Board? -> callBack.onResult(board) }, { callBack.onResult(null) }) {
            val response = NgaApi.get().get("https://bbs.nga.cn/forum.php?&__output=8&key=" + StringUtils.encodeUrl(boardName, "gbk"))
            try {
                val obj = NgaJson.parseObject(response)!!.getJSONObject("data")!!.getJSONObject("0")!!
                val fid = obj.getIntValue("fid")
                BoardModel.getInstance().findBoard(fid.toString()) ?: Board(fid, obj.getString("name"))
            } catch (e: Exception) {
                null
            }
        }
    }
}
