package gov.anzong.androidnga.network.bean

/** 主题列表接口的响应，按接口字段名绑定。 */
class TopicListBean {

    var data: DataBean? = null

    var encode: String? = null

    var time = 0

    class DataBean {

        var __CU: CUBean? = null

        var __GLOBAL: String? = null

        var __F: FBean? = null

        var __ROWS: String? = null

        var __T: Map<String, TBean>? = null

        var __T__ROWS = 0

        var __T__ROWS_PAGE = 0

        var __R__ROWS_PAGE = 0

        /** 当前用户。 */
        class CUBean {

            var uid = 0

            var group_bit = 0

            var admincheck: String? = null

            var rvrc = 0
        }

        /** 版块信息与子版块。 */
        class FBean {

            var topped_topic: String? = null

            var sub_forums: String? = null

            var fid = 0

            @JvmField
            var name: String? = null
        }

        /** 一个主题。 */
        class TBean {

            var tid = 0

            var fid = 0

            var quote_from = 0

            var quote_to: String? = null

            var topic_misc: String? = null

            var author: String? = null

            var authorid: String? = null

            var subject: String? = null

            var type = 0

            var postdate = 0

            var lastpost = 0

            var lastposter: String? = null

            var replies = 0

            var lastmodify = 0

            var recommend = 0

            var titlefont: String? = null

            var admin_ui = 0

            var tpcurl: String? = null

            var __P: PBean? = null

            var parent: Map<String, String>? = null

            @JvmField
            var topic_misc_var: Map<String, String>? = null

            override fun toString(): String =
                "TBean{tid=$tid, fid=$fid, subject='$subject', type=$type, recommend=$recommend}"

            /** 搜索回复时匹配到的回复。 */
            class PBean {

                var tid = 0

                var pid = 0

                var authorid: String? = null

                var type = 0

                var postdate = 0

                var subject: String? = null

                var content: String? = null
            }
        }
    }
}
