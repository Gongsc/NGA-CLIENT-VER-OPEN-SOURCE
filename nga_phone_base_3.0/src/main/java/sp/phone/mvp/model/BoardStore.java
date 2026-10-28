package sp.phone.mvp.model;

import java.util.List;

import sp.phone.mvp.model.entity.Board;
import sp.phone.mvp.model.entity.BoardCategory;

/** 版块列表与本地版块收藏。 */
public interface BoardStore {

    void addBookmark(Board board);

    void addBookmark(int fid, int stid, String boardName);

    void addBookmark(Board.BoardKey boardKey, String boardName);

    void removeBookmark(int fid, int stid);

    boolean isBookmark(int fid, int stid);

    int getCategorySize();

    BoardCategory getBoardCategory(int index);

    List<BoardCategory> getBoardCategories();

    String getBoardName(Board.BoardKey boardKey);

    String getBoardName(int fid, int stid);

    BoardCategory getBookmarkCategory();
}
