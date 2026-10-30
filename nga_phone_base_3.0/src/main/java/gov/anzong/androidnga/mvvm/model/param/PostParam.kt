package gov.anzong.androidnga.mvvm.model.param

import android.os.Parcel
import android.os.Parcelable
import java.net.URLEncoder

/** 发帖、回复与编辑的参数；附件只在本次编辑中保存，不随页面重建恢复。 */
class PostParam : Parcelable {

    var postPid: String? = null

    var postAction: String? = null

    var postFid = 0

    val postTid: String?

    var postSubject: String?

    var postContent: String?

    var isAnonymous = false

    @get:Synchronized
    @set:Synchronized
    var authCode: String? = null

    var stid: String? = null

    private var attachmentList: StringBuilder? = null

    private var attachmentCheckList: StringBuilder? = null

    /** 已表单编码的附件字段，多个附件以 %09 分隔；没有附件时为 null。 */
    val attachments: String?
        get() = attachmentList?.toString()

    val attachmentsCheck: String?
        get() = attachmentCheckList?.toString()

    constructor(tid: String?, subject: String?, content: String?) {
        postTid = tid
        postSubject = subject
        postContent = content
    }

    private constructor(parcel: Parcel) {
        postPid = parcel.readString()
        postAction = parcel.readString()
        postFid = parcel.readInt()
        postTid = parcel.readString()
        postSubject = parcel.readString()
        postContent = parcel.readString()
        isAnonymous = parcel.readByte().toInt() != 0
        authCode = parcel.readString()
        stid = parcel.readString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(postPid)
        dest.writeString(postAction)
        dest.writeInt(postFid)
        dest.writeString(postTid)
        dest.writeString(postSubject)
        dest.writeString(postContent)
        dest.writeByte(if (isAnonymous) 1 else 0)
        dest.writeString(authCode)
        dest.writeString(stid)
    }

    override fun describeContents(): Int = 0

    fun appendAttachment(attachment: String?, attachmentCheck: String?) {
        val separator = URLEncoder.encode("\t", "GBK")
        val list = attachmentList ?: StringBuilder().also { attachmentList = it }
        val checks = attachmentCheckList ?: StringBuilder().also { attachmentCheckList = it }
        list.append(separator).append(attachment)
        checks.append(separator).append(attachmentCheck)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<PostParam> {
            override fun createFromParcel(parcel: Parcel) = PostParam(parcel)

            override fun newArray(size: Int) = arrayOfNulls<PostParam>(size)
        }
    }
}
