package sp.phone.mvp.contract;

import sp.phone.common.User;
import sp.phone.mvp.model.entity.Board;
import sp.phone.mvp.model.entity.BoardCategory;

import java.util.List;

/** Created by Justwen on 2017/6/29. */
public interface BoardContract {

    interface Presenter {

        void loadBoardInfo();

        boolean addBoard(String fid, String stid);

        void toggleUser(List<User> userList);

        void toTopicListPage(int position, String fidString);

        void notifyDataSetChanged();

        void startUserProfile(String uid);

        void startLogin();

        BoardCategory getBookmarkCategory();

        List<BoardCategory> getBoardCategories();

        void addBookmarkBoard(int fid, int stid, String name);

        void showTopicList(Board board);

        void showTopicList(int fid, int stid, String boardName);

        void showTopicList(String url);

        void showTopicContent(String url);
    }

    interface View {

        int switchToNextUser();

        void jumpToLogin();

        void updateHeaderView();

        void notifyDataSetChanged();

        int getCurrentItem();
    }

    interface Model {

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
}
