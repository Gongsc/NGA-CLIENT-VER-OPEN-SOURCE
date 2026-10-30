package gov.anzong.androidnga.mvvm.model

import gov.anzong.androidnga.mvvm.model.entity.Board
import gov.anzong.androidnga.mvvm.model.entity.Board.BoardKey
import gov.anzong.androidnga.mvvm.model.entity.BoardCategory

/** 版块列表与本地版块收藏。 */
interface BoardStore {

    fun addBookmark(board: Board)

    fun addBookmark(fid: Int, stid: Int, boardName: String?)

    fun addBookmark(boardKey: BoardKey, boardName: String?)

    fun removeBookmark(fid: Int, stid: Int)

    fun isBookmark(fid: Int, stid: Int): Boolean

    fun getCategorySize(): Int

    fun getBoardCategory(index: Int): BoardCategory

    fun getBoardCategories(): List<BoardCategory>

    fun getBoardName(boardKey: BoardKey): String?

    fun getBoardName(fid: Int, stid: Int): String?

    fun getBookmarkCategory(): BoardCategory
}
