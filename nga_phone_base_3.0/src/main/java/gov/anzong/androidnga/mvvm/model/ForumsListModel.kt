package gov.anzong.androidnga.mvvm.model

/**
 * 版块目录接口的响应。
 * Created by elrond on 2017/9/29.
 */
class ForumsListModel {

    var code = 0

    var msg: String? = null

    var result: ArrayList<Result>? = null

    class Result {

        var id: String? = null

        var name: String? = null

        var groups: ArrayList<Group>? = null
    }

    class Group {

        var id: String? = null

        var name: String? = null

        var forums: ArrayList<Forum>? = null
    }

    class Forum {

        var id = 0

        var name: String? = null

        var stid = 0

        override fun equals(other: Any?): Boolean =
            this === other || other is Forum && javaClass == other.javaClass && id == other.id && name == other.name

        override fun hashCode(): Int = 31 * id + (name?.hashCode() ?: 0)
    }
}
