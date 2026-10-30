package gov.anzong.androidnga.mvvm.model.entity

import android.os.Parcel
import android.os.Parcelable

/**
 * 回复提醒：被回复的主题与楼层。
 * Created by Justwen on 2018/3/10.
 */
class RecentReplyInfo : NotificationInfo {

    val title: String?

    val pidStr: String

    val tidStr: String?

    constructor(type: Int, userName: String?, title: String?, pidStr: String, tidStr: String?) : super(type, userName) {
        this.title = title
        this.pidStr = pidStr
        this.tidStr = tidStr
    }

    private constructor(parcel: Parcel) : super(parcel) {
        title = parcel.readString()
        pidStr = parcel.readString().orEmpty()
        tidStr = parcel.readString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(title)
        dest.writeString(pidStr)
        dest.writeString(tidStr)
    }

    override fun describeContents(): Int = 0

    override fun equals(other: Any?): Boolean = other is RecentReplyInfo && pidStr == other.pidStr

    override fun hashCode(): Int = pidStr.hashCode()

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<RecentReplyInfo> {
            override fun createFromParcel(parcel: Parcel) = RecentReplyInfo(parcel)

            override fun newArray(size: Int) = arrayOfNulls<RecentReplyInfo>(size)
        }
    }
}
