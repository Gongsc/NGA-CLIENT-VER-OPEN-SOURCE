package gov.anzong.androidnga.mvvm.model.param

import android.os.Parcel
import android.os.Parcelable

/** 写短消息或回复会话的参数。 */
class MessagePostParam : Parcelable {

    val action: String?

    val mid: Int

    var postSubject: String?

    var postContent: String? = null

    var recipient: String? = null

    constructor() {
        action = "new"
        mid = 0
        postSubject = null
    }

    constructor(action: String?, mid: Int, postSubject: String?) {
        this.action = action
        this.mid = mid
        this.postSubject = postSubject
    }

    private constructor(parcel: Parcel) {
        action = parcel.readString()
        mid = parcel.readInt()
        postSubject = parcel.readString()
        postContent = parcel.readString()
        recipient = parcel.readString()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(action)
        dest.writeInt(mid)
        dest.writeString(postSubject)
        dest.writeString(postContent)
        dest.writeString(recipient)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<MessagePostParam> {
            override fun createFromParcel(parcel: Parcel) = MessagePostParam(parcel)

            override fun newArray(size: Int) = arrayOfNulls<MessagePostParam>(size)
        }
    }
}
