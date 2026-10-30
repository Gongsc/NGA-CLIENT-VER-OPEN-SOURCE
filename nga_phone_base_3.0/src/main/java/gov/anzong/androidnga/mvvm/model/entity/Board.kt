package gov.anzong.androidnga.mvvm.model.entity

import android.os.Parcel
import android.os.Parcelable

/** 版块：版块编号或合集编号（stid）、名称与版头主题。 */
open class Board : Parcelable {

    /**
     * 版块的查找键。fid 或 stid 任一非零且相等即视为同一版块。
     */
    class BoardKey(val fid: Int, val stid: Int) : Parcelable {

        private constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readInt())

        override fun equals(other: Any?): Boolean =
            other is BoardKey && (fid != 0 && fid == other.fid || stid != 0 && stid == other.stid)

        override fun hashCode(): Int = toString().hashCode()

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(fid)
            dest.writeInt(stid)
        }

        override fun toString(): String = if (stid != 0) "stid=$stid" else "fid=$fid"

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<BoardKey> {
                override fun createFromParcel(parcel: Parcel) = BoardKey(parcel)

                override fun newArray(size: Int) = arrayOfNulls<BoardKey>(size)
            }
        }
    }

    var fid = 0

    var name: String? = null

    /** 子类可以覆盖 [stid]；构造与序列化只读写这里的值。 */
    private var std = 0

    open var stid: Int
        get() = std
        set(value) {
            std = value
        }

    var boardKey: BoardKey? = null
        private set

    var boardHead: String? = null

    constructor()

    constructor(fid: Int, stid: Int, name: String?) {
        this.fid = fid
        this.name = name
        std = stid
        boardKey = BoardKey(fid, stid)
    }

    constructor(fid: Int, name: String?) : this(fid, 0, name)

    constructor(boardKey: BoardKey, name: String?) {
        fid = boardKey.fid
        this.name = name
        std = boardKey.stid
        this.boardKey = boardKey
    }

    protected constructor(parcel: Parcel) {
        fid = parcel.readInt()
        name = parcel.readString()
        std = parcel.readInt()
        boardKey = parcel.readParcelable(BoardKey::class.java.classLoader, BoardKey::class.java)
        boardHead = parcel.readString()
    }

    override fun equals(other: Any?): Boolean = other is Board && boardKey == other.boardKey

    override fun hashCode(): Int = if (fid != 0) fid else stid

    fun toUrlString(): String = boardKey.toString()

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(fid)
        dest.writeString(name)
        dest.writeInt(std)
        dest.writeParcelable(boardKey, flags)
        dest.writeString(boardHead)
    }

    override fun toString(): String =
        "Board{mFid=$fid, mName='$name', mStd=$std, mBoardKey=$boardKey, mBoardHead='$boardHead'}"

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<Board> {
            override fun createFromParcel(parcel: Parcel) = Board(parcel)

            override fun newArray(size: Int) = arrayOfNulls<Board>(size)
        }
    }
}
