package gov.anzong.androidnga.mvvm.model.param

import android.os.Parcel
import android.os.Parcelable

/**
 * 主题详情一页的请求参数。
 * Created by Justwen on 2017/7/9.
 */
class ArticleListParam() : Parcelable, Cloneable {

    @JvmField var pid = 0

    @JvmField var tid = 0

    @JvmField var authorId = 0

    @JvmField var page = 0

    @JvmField var searchPost = 0

    @JvmField var title: String? = null

    @JvmField var content: String? = null

    @JvmField var topicInfo: String? = null

    @JvmField var loadCache = false

    private constructor(parcel: Parcel) : this() {
        pid = parcel.readInt()
        tid = parcel.readInt()
        authorId = parcel.readInt()
        page = parcel.readInt()
        searchPost = parcel.readInt()
        title = parcel.readString()
        content = parcel.readString()
        topicInfo = parcel.readString()
        loadCache = parcel.readInt() == 1
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(pid)
        dest.writeInt(tid)
        dest.writeInt(authorId)
        dest.writeInt(page)
        dest.writeInt(searchPost)
        dest.writeString(title)
        dest.writeString(content)
        dest.writeString(topicInfo)
        dest.writeInt(if (loadCache) 1 else 0)
    }

    override fun describeContents(): Int = 0

    /** 请求同一页时相等；标题与缓存标记不参与比较。 */
    override fun equals(other: Any?): Boolean =
        other is ArticleListParam && pid == other.pid && tid == other.tid && authorId == other.authorId &&
            page == other.page && content == other.content && searchPost == other.searchPost

    override fun hashCode(): Int = java.util.Objects.hash(pid, tid, authorId, page, content, searchPost)

    public override fun clone(): Any = super.clone()

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<ArticleListParam> {
            override fun createFromParcel(parcel: Parcel) = ArticleListParam(parcel)

            override fun newArray(size: Int) = arrayOfNulls<ArticleListParam>(size)
        }
    }
}
