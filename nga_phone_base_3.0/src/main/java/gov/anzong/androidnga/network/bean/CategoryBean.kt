package gov.anzong.androidnga.network.bean

import gov.anzong.androidnga.common.base.JavaBean

/**
 * 内置版块列表（assets/json/category.json）中的一个分区，如
 * `{"id":"wow","name":"魔兽世界","sub":[{"name":"职业讨论区","content":[{"fid":181,"name":"铁血沙场","info":"战士"}]}]}`。
 */
class CategoryBean : JavaBean {

    var id: String? = null

    var name: String? = null

    var sub: List<SubBean>? = null

    class SubBean {

        var name: String? = null

        var content: List<ContentBean>? = null

        class ContentBean {

            var fid = 0

            var name: String? = null

            var info: String? = null

            /** 简称，显示时优先使用。 */
            var nameS: String? = null

            var infoS: String? = null

            var stid = 0

            /** 版头主题编号。 */
            var head: String? = null

            override fun toString(): String =
                "ContentBean{fid=$fid, name='$name', info='$info', nameS='$nameS', infoS='$infoS', stid=$stid, head='$head'}"
        }
    }
}
