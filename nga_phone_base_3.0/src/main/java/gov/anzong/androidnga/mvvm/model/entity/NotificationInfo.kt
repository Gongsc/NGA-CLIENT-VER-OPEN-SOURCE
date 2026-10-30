package gov.anzong.androidnga.mvvm.model.entity

import android.os.Parcel
import android.os.Parcelable

/**
 * 一条论坛提醒。
 * Created by Justwen on 2018/3/10.
 */
open class NotificationInfo : Parcelable {

    val type: Int

    val userName: String?

    var isUnread = false

    var timeStamp: String? = null

    var userId: String? = null

    constructor(type: Int, userName: String?) {
        this.type = type
        this.userName = userName
    }

    protected constructor(parcel: Parcel) {
        type = parcel.readInt()
        userName = parcel.readString()
        isUnread = parcel.readByte().toInt() != 0
        timeStamp = parcel.readString()
        userId = parcel.readString()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(type)
        dest.writeString(userName)
        dest.writeByte(if (isUnread) 1 else 0)
        dest.writeString(timeStamp)
        dest.writeString(userId)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<NotificationInfo> {
            override fun createFromParcel(parcel: Parcel) = NotificationInfo(parcel)

            override fun newArray(size: Int) = arrayOfNulls<NotificationInfo>(size)
        }
    }
}
