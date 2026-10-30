package gov.anzong.androidnga.mvvm.model.param

/**
 * 论坛接口与页面参数的键名。
 * Created by Justwen on 2017/11/26.
 */
object ParamKey {

    // 版面ID  整数 或 逗号分隔的整数
    const val KEY_FID = "fid"

    // 有些子版面是stid
    const val KEY_STID = "stid"

    // 主题作者用户id  整数
    const val KEY_AUTHOR_ID = "authorid"

    // 搜索关键字  字符串urlencode
    const val KEY_KEY = "key"

    // 页  整数
    const val KEY_PAGE = "page"

    // 搜索的版面组  取值为user时表示全部用户版 无此参数为全部非用户版
    const val KEY_FID_GROUP = "fidgroup"

    // 收藏的主题  为1时显示收藏的主题
    const val KEY_FAVOR = "favor"

    // 推荐精华加分的主题  为1时显示推荐 精华 加分的主题
    const val KEY_RECOMMEND = "recommend"

    // 24 小时热帖
    const val KEY_TWENTYFOUR = "twentyfour"

    // 主题id  整数
    const val KEY_TID = "tid"

    // 回复id  整数
    const val KEY_PID = "pid"

    const val KEY_ACTION = "action"

    const val KEY_SEARCH_POST = "searchpost"

    const val KEY_CONTENT = "content"

    const val KEY_AUTHOR = "author"

    const val KEY_TITLE = "board_name"

    const val KEY_PARAM = "requestParam"

    const val BOARD_HEAD = "board_head"
}
