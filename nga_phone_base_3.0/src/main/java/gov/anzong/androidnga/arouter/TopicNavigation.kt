package gov.anzong.androidnga.arouter

import android.content.Context
import android.content.Intent
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.network.ApiConstants
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.mvvm.model.TopicHistoryManager
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo
import gov.anzong.androidnga.mvvm.model.param.ArticleListParam
import gov.anzong.androidnga.mvvm.model.param.ParamKey
import gov.anzong.androidnga.mvvm.model.param.TopicListParam
import gov.anzong.androidnga.util.StringUtils

/** 打开主题列表中的一项：镜像版面与合集进入对应的主题列表，其余进入主题内容并记入浏览历史。 */
object TopicNavigation {

    @JvmStatic
    fun open(context: Context, info: ThreadPageInfo, requestParam: TopicListParam) {
        when {
            info.isMirrorBoard -> AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                .withInt(ParamKey.KEY_FID, info.fid)
                .withString(ParamKey.KEY_TITLE, info.subject)
                .navigation(context)
            info.type and ApiConstants.MASK_TYPE_ASSEMBLE == ApiConstants.MASK_TYPE_ASSEMBLE -> {
                val param = TopicListParam().apply {
                    title = info.subject
                    stid = info.tid
                }
                AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                    .withParcelable(ParamKey.KEY_PARAM, param)
                    .navigation(context)
            }
            else -> {
                val param = ArticleListParam().apply {
                    tid = info.tid
                    page = info.page
                    title = StringUtils.unEscapeHtml(info.subject)
                    if (requestParam.searchPost != 0) {
                        pid = info.pid
                        authorId = info.authorId
                        searchPost = requestParam.searchPost
                    }
                    topicInfo = NgaJson.toJSONString(info)
                }
                context.startActivity(Intent(context, PhoneConfiguration.getInstance().articleActivityClass)
                    .putExtra(ParamKey.KEY_PARAM, param))
                TopicHistoryManager.getInstance().addTopicHistory(info)
            }
        }
    }

    /** 从浏览历史打开主题，回到上次读到的页。 */
    @JvmStatic
    fun openHistory(context: Context, topic: ThreadPageInfo) {
        val param = ArticleListParam().apply {
            tid = topic.tid; page = topic.page.coerceAtLeast(1); title = topic.subject
            topicInfo = NgaJson.toJSONString(topic)
        }
        context.startActivity(Intent(context, PhoneConfiguration.getInstance().articleActivityClass).putExtra(ParamKey.KEY_PARAM, param))
    }
}
