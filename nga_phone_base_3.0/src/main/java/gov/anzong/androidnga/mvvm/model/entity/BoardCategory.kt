package gov.anzong.androidnga.mvvm.model.entity

import android.os.Parcel
import android.os.Parcelable
import gov.anzong.androidnga.mvvm.model.entity.Board.BoardKey

/** 版块分类，可包含子分类；“我的收藏”也是一个分类。 */
class BoardCategory : Parcelable {

    private val boards: MutableList<Board>

    private val boardMap = HashMap<BoardKey?, Board>()

    val name: String?

    var isBookmarkCategory = false

    var subCategoryList: MutableList<BoardCategory>? = null
        private set

    val boardList: List<Board>
        get() = boards

    constructor(name: String?) {
        boards = ArrayList()
        this.name = name
    }

    private constructor(parcel: Parcel) {
        boards = parcel.createTypedArrayList(Board.CREATOR) ?: ArrayList()
        name = parcel.readString()
        isBookmarkCategory = parcel.readByte().toInt() != 0
        for (board in boards) {
            boardMap[board.boardKey] = board
        }
        subCategoryList = parcel.createTypedArrayList(CREATOR)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(boards)
        dest.writeString(name)
        dest.writeByte(if (isBookmarkCategory) 1 else 0)
        dest.writeTypedList(subCategoryList)
    }

    override fun describeContents(): Int = 0

    fun addSubCategory(category: BoardCategory) {
        val list = subCategoryList ?: ArrayList<BoardCategory>().also { subCategoryList = it }
        list.add(category)
    }

    fun getSubCategory(index: Int): BoardCategory = subCategoryList!![index]

    fun size(): Int = boards.size

    fun addBoards(data: List<Board>) {
        data.forEach(::addBoard)
    }

    fun addBoard(board: Board) {
        boards.add(board)
        boardMap[board.boardKey] = board
    }

    fun removeAllBoards() {
        boards.clear()
        boardMap.clear()
    }

    fun removeBoard(board: Board) {
        boards.remove(board)
        boardMap.remove(board.boardKey)
    }

    fun removeBoard(boardKey: BoardKey): Boolean {
        val board = boardMap.remove(boardKey) ?: return false
        return boards.remove(board)
    }

    fun getBoard(index: Int): Board = boards[index]

    /** 有子分类时在子分类中查找。 */
    fun getBoard(boardKey: BoardKey): Board? {
        val subCategories = subCategoryList ?: return boardMap[boardKey]
        return subCategories.firstNotNullOfOrNull { it.getBoard(boardKey) }
    }

    operator fun contains(board: Board): Boolean = boards.contains(board)

    operator fun contains(boardKey: BoardKey): Boolean = boardMap.containsKey(boardKey)

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<BoardCategory> {
            override fun createFromParcel(parcel: Parcel) = BoardCategory(parcel)

            override fun newArray(size: Int) = arrayOfNulls<BoardCategory>(size)
        }
    }
}
