package sp.phone.mvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * @author yangyihang
 */
public class ArticleShareViewModel extends ViewModel {

    public static final class ReaderControls {
        public final int page;
        public final boolean expanded;

        public ReaderControls(int page, boolean expanded) {
            this.page = page;
            this.expanded = expanded;
        }
    }

    private final MutableLiveData<ReaderControls> mReaderControls = new MutableLiveData<>();

    public LiveData<ReaderControls> getReaderControls() { return mReaderControls; }

    public void setReaderControls(int page, boolean expanded) {
        mReaderControls.setValue(new ReaderControls(page, expanded));
    }

    private final MutableLiveData<Integer> mTopicId = new MutableLiveData<>();

    public LiveData<Integer> getTopicId() {
        return mTopicId;
    }

    public void setTopicId(int tid) {
        mTopicId.setValue(tid);
    }

    private final MutableLiveData<String> mTitle = new MutableLiveData<>();

    public LiveData<String> getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle.setValue(title);
    }

    private MutableLiveData<Integer> mReplyCount = new MutableLiveData<>();

    private MutableLiveData<Integer> mRefreshPage = new MutableLiveData<>();

    private MutableLiveData<Integer> mCachePage = new MutableLiveData<>();

    private MutableLiveData<String> mTopicOwner = new MutableLiveData<>();

    public MutableLiveData<Integer> getReplyCount() {
        return mReplyCount;
    }

    public void setReplyCount(int replyCount) {
        mReplyCount.setValue(replyCount);
    }

    public MutableLiveData<Integer> getRefreshPage() {
        return mRefreshPage;
    }

    public void setRefreshPage(int refreshPage) {
        mRefreshPage.setValue(refreshPage);
    }

    public MutableLiveData<Integer> getCachePage() {
        return mCachePage;
    }

    public void setCachePage(int cachePage) {
        mCachePage.setValue(cachePage);
    }

    public LiveData<String> getTopicOwner() {
        return mTopicOwner;
    }

    public void setTopicOwner(String owner) {
        mTopicOwner.setValue(owner);
    }
}
