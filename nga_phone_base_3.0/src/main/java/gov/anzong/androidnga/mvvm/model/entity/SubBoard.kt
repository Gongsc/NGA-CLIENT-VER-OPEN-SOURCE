package gov.anzong.androidnga.mvvm.model.entity

import android.os.Parcel
import android.os.Parcelable

/**
 * 版块的子版块或合集，可在列表中显示或隐藏。
 * Created by Justwen on 2018/1/31.
 */
class SubBoard : Board {

    var tidStr: String? = null

    /** 与 [Board] 中的值分开保存，与旧版一致。 */
    private var subStid = 0

    override var stid: Int
        get() = subStid
        set(value) {
            subStid = value
        }

    var type = 0

    var isChecked = true

    var description: String? = null

    var parentFidStr: String? = null

    constructor()

    private constructor(parcel: Parcel) : super(parcel) {
        tidStr = parcel.readString()
        subStid = parcel.readInt()
        type = parcel.readInt()
        isChecked = parcel.readByte().toInt() != 0
        description = parcel.readString()
        parentFidStr = parcel.readString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(tidStr)
        dest.writeInt(subStid)
        dest.writeInt(type)
        dest.writeByte(if (isChecked) 1 else 0)
        dest.writeString(description)
        dest.writeString(parentFidStr)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<SubBoard> {
            override fun createFromParcel(parcel: Parcel) = SubBoard(parcel)

            override fun newArray(size: Int) = arrayOfNulls<SubBoard>(size)
        }
    }
}
