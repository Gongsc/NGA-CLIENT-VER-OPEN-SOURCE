package sp.phone.mvp.model;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;

import sp.phone.http.bean.CategoryBean;
import sp.phone.http.favorites.ForumFavoriteBoard;
import sp.phone.http.favorites.ForumFavoritesRepository;
import sp.phone.mvp.contract.BoardContract;
import sp.phone.mvp.model.entity.Board;
import sp.phone.mvp.model.entity.BoardCategory;
import sp.phone.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** Created by Justwen on 2019/6/23. */
public class BoardModel extends BaseModel implements BoardContract.Model {

    private List<BoardCategory> mBoardCategoryList = new ArrayList<>();

    private BoardCategory mBookmarkCategory;

    private BoardModel() {
        mBookmarkCategory = new BoardCategory("我的收藏");
        mBookmarkCategory.setBookmarkCategory(true);
        mBoardCategoryList.add(mBookmarkCategory);
        mBoardCategoryList.addAll(loadPreloadBoards());
    }

    public Board findBoard(String fid) {
        Board.BoardKey boardKey = new Board.BoardKey(Integer.parseInt(fid), 0);
        for (BoardCategory boardCategory : mBoardCategoryList) {
            Board board = boardCategory.getBoard(boardKey);
            if (board != null) {
                return board;
            }
        }
        return null;
    }

    private List<BoardCategory> loadPreloadBoards() {
        String categoryJson = StringUtils.getStringFromAssets("json/category.json");
        List<CategoryBean> beans = JSON.parseArray(categoryJson, CategoryBean.class);
        List<BoardCategory> categories = new ArrayList<>();

        for (CategoryBean categoryBean : beans) {
            BoardCategory category = new BoardCategory(categoryBean.getName());
            for (CategoryBean.SubBean subBean : categoryBean.getSub()) {
                BoardCategory subCategory = new BoardCategory(subBean.getName());
                for (CategoryBean.SubBean.ContentBean contentBean : subBean.getContent()) {
                    String boardName;
                    if (TextUtils.isEmpty(contentBean.getNameS())) {
                        boardName = contentBean.getName();
                    } else {
                        boardName = contentBean.getNameS();
                    }

                    Board board = new Board(contentBean.getFid(), contentBean.getStid(), boardName);
                    board.setBoardHead(contentBean.getHead());
                    subCategory.addBoard(board);
                }
                category.addSubCategory(subCategory);
            }
            categories.add(category);
        }
        return categories;
    }

    @Override
    public void addBookmark(Board board) {
        ForumFavoritesRepository.getInstance()
                .add(board.getFid(), board.getStid(), board.getName(), board.getBoardHead());
    }

    @Override
    public void addBookmark(int fid, int stid, String boardName) {
        ForumFavoritesRepository.getInstance().add(fid, stid, boardName, null);
    }

    @Override
    public void addBookmark(Board.BoardKey boardKey, String boardName) {
        addBookmark(new Board(boardKey, boardName));
    }

    @Override
    public void removeBookmark(int fid, int stid) {
        ForumFavoritesRepository.getInstance().remove(fid, stid);
    }

    @Override
    public boolean isBookmark(int fid, int stid) {
        return ForumFavoritesRepository.getInstance().contains(fid, stid);
    }

    @Override
    public int getCategorySize() {
        return mBoardCategoryList.size();
    }

    @Override
    public BoardCategory getBoardCategory(int index) {
        if (index == 0) getBookmarkCategory();
        return mBoardCategoryList.get(index);
    }

    @Override
    public List<BoardCategory> getBoardCategories() {
        getBookmarkCategory();
        return mBoardCategoryList;
    }

    @Override
    public String getBoardName(Board.BoardKey boardKey) {
        for (BoardCategory boardCategory : mBoardCategoryList) {
            Board board = boardCategory.getBoard(boardKey);
            if (board != null) {
                return board.getName();
            }
        }
        return null;
    }

    @Override
    public String getBoardName(int fid, int stid) {
        return getBoardName(new Board.BoardKey(fid, stid));
    }

    @Override
    public BoardCategory getBookmarkCategory() {
        mBookmarkCategory.removeAllBoards();
        for (ForumFavoriteBoard board :
                ForumFavoritesRepository.getInstance().current().getBoards()) {
            Board entry = new Board(board.getFid(), board.getStid(), board.getName());
            entry.setBoardHead(board.getHead());
            mBookmarkCategory.addBoard(entry);
        }
        return mBookmarkCategory;
    }

    private static class SingletonHolder {
        private static BoardModel sInstance = new BoardModel();
    }

    public static BoardModel getInstance() {
        return SingletonHolder.sInstance;
    }
}
