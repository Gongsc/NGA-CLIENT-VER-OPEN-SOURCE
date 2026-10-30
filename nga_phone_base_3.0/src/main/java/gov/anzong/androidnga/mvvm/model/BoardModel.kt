package gov.anzong.androidnga.mvvm.model

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.network.bean.CategoryBean
import gov.anzong.androidnga.network.favorites.ForumFavoritesRepository
import gov.anzong.androidnga.mvvm.model.entity.Board
import gov.anzong.androidnga.mvvm.model.entity.Board.BoardKey
import gov.anzong.androidnga.mvvm.model.entity.BoardCategory
import gov.anzong.androidnga.util.StringUtils

/**
 * 内置版块列表与论坛收藏；收藏分类每次读取时按当前账号的论坛收藏重建。
 * Created by Justwen on 2019/6/23.
 */
class BoardModel private constructor() : BoardStore {

    private val bookmarkCategory = BoardCategory("我的收藏").apply { isBookmarkCategory = true }

    private val boardCategoryList: MutableList<BoardCategory> = mutableListOf(bookmarkCategory).apply {
        addAll(loadPreloadBoards())
    }

    fun findBoard(fid: String): Board? {
        val boardKey = BoardKey(fid.toInt(), 0)
        return boardCategoryList.firstNotNullOfOrNull { it.getBoard(boardKey) }
    }

    private fun loadPreloadBoards(): List<BoardCategory> {
        val beans = NgaJson.parseArray(StringUtils.getStringFromAssets("json/category.json"), CategoryBean::class.java)
        return beans.orEmpty().map { categoryBean ->
            BoardCategory(categoryBean.name).apply {
                for (subBean in categoryBean.sub.orEmpty()) {
                    val subCategory = BoardCategory(subBean.name)
                    for (content in subBean.content.orEmpty()) {
                        val boardName = if (content.nameS.isNullOrEmpty()) content.name else content.nameS
                        subCategory.addBoard(Board(content.fid, content.stid, boardName).apply { boardHead = content.head })
                    }
                    addSubCategory(subCategory)
                }
            }
        }
    }

    override fun addBookmark(board: Board) {
        ForumFavoritesRepository.getInstance().add(board.fid, board.stid, board.name.orEmpty(), board.boardHead)
    }

    override fun addBookmark(fid: Int, stid: Int, boardName: String?) {
        ForumFavoritesRepository.getInstance().add(fid, stid, boardName.orEmpty(), null)
    }

    override fun addBookmark(boardKey: BoardKey, boardName: String?) {
        addBookmark(Board(boardKey, boardName))
    }

    override fun removeBookmark(fid: Int, stid: Int) {
        ForumFavoritesRepository.getInstance().remove(fid, stid)
    }

    override fun isBookmark(fid: Int, stid: Int): Boolean = ForumFavoritesRepository.getInstance().contains(fid, stid)

    override fun getCategorySize(): Int = boardCategoryList.size

    override fun getBoardCategory(index: Int): BoardCategory {
        if (index == 0) getBookmarkCategory()
        return boardCategoryList[index]
    }

    override fun getBoardCategories(): List<BoardCategory> {
        getBookmarkCategory()
        return boardCategoryList
    }

    override fun getBoardName(boardKey: BoardKey): String? =
        boardCategoryList.firstNotNullOfOrNull { it.getBoard(boardKey) }?.name

    override fun getBoardName(fid: Int, stid: Int): String? = getBoardName(BoardKey(fid, stid))

    override fun getBookmarkCategory(): BoardCategory {
        bookmarkCategory.removeAllBoards()
        for (board in ForumFavoritesRepository.getInstance().current().boards) {
            bookmarkCategory.addBoard(Board(board.fid, board.stid, board.name).apply { boardHead = board.head })
        }
        return bookmarkCategory
    }

    companion object {

        private val INSTANCE by lazy { BoardModel() }

        @JvmStatic
        fun getInstance(): BoardModel = INSTANCE
    }
}
