package gov.anzong.androidnga.core.corebuild

import gov.anzong.androidnga.core.data.HtmlData

/**
 * Created by Justwen on 2018/8/28.
 */
object HtmlVoteBuilder : IHtmlBuild {

    override fun build(htmlData: HtmlData, images: MutableList<String>): CharSequence =
        if (htmlData.vote.isNullOrEmpty()) "" else "<br/><hr/>本楼有投票/投注内容,长按本楼在菜单中点击投票/投注按钮"
}
