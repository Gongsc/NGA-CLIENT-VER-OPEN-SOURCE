package gov.anzong.androidnga.mvvm.model.param

import android.os.Parcel
import android.os.Parcelable

/**
 * 主题列表的请求参数：版块、搜索、收藏、精华与热帖等。
 * Created by Justwen on 2017/6/3.
 */
class TopicListParam() : Cloneable, Parcelable {

    @JvmField var authorId = 0

    @JvmField var searchPost = 0

    @JvmField var favor = 0

    @JvmField var content = 0

    @JvmField var fid = 0

    @JvmField var key: String? = null

    @JvmField var fidGroup: String? = null

    @JvmField var author: String? = null

    @JvmField var recommend = 0

    @JvmField var twentyfour = 0

    @JvmField var title: String? = null

    @JvmField var stid = 0

    @JvmField var loadCache = false

    @JvmField var boardHead: String? = null

    private constructor(parcel: Parcel) : this() {
        authorId = parcel.readInt()
        searchPost = parcel.readInt()
        favor = parcel.readInt()
        content = parcel.readInt()
        fid = parcel.readInt()
        key = parcel.readString()
        fidGroup = parcel.readString()
        author = parcel.readString()
        recommend = parcel.readInt()
        twentyfour = parcel.readInt()
        title = parcel.readString()
        stid = parcel.readInt()
        loadCache = parcel.readInt() == 1
        boardHead = parcel.readString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(authorId)
        dest.writeInt(searchPost)
        dest.writeInt(favor)
        dest.writeInt(content)
        dest.writeInt(fid)
        dest.writeString(key)
        dest.writeString(fidGroup)
        dest.writeString(author)
        dest.writeInt(recommend)
        dest.writeInt(twentyfour)
        dest.writeString(title)
        dest.writeInt(stid)
        dest.writeInt(if (loadCache) 1 else 0)
        dest.writeString(boardHead)
    }

    override fun describeContents(): Int = 0

    public override fun clone(): Any = super.clone()

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<TopicListParam> {
            override fun createFromParcel(parcel: Parcel) = TopicListParam(parcel)

            override fun newArray(size: Int) = arrayOfNulls<TopicListParam>(size)
        }
    }
}
