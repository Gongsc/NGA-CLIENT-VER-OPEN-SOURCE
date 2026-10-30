package gov.anzong.androidnga.settings

import android.content.Context
import android.content.SharedPreferences
import gov.anzong.androidnga.activity.ArticleListActivity
import gov.anzong.androidnga.activity.LoginActivity
import gov.anzong.androidnga.activity.MessageDetailActivity
import gov.anzong.androidnga.activity.PostActivity
import gov.anzong.androidnga.activity.ProfileActivity
import gov.anzong.androidnga.activity.SignPostActivity
import gov.anzong.androidnga.activity.TopicListActivity
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.base.util.DeviceUtils
import gov.anzong.androidnga.base.util.PreferenceUtils
import gov.anzong.androidnga.common.PreferenceKey

/** 常用设置的内存副本，设置变化时随之更新。 */
class PhoneConfiguration private constructor() : SharedPreferences.OnSharedPreferenceChangeListener {

    @JvmField
    var topicActivityClass: Class<*> = TopicListActivity::class.java

    @JvmField
    var articleActivityClass: Class<*> = ArticleListActivity::class.java

    @JvmField
    var postActivityClass: Class<*> = PostActivity::class.java

    @JvmField
    var signPostActivityClass: Class<*> = SignPostActivity::class.java

    @JvmField
    var profileActivityClass: Class<*> = ProfileActivity::class.java

    @JvmField
    var loginActivityClass: Class<*> = LoginActivity::class.java

    @JvmField
    var messageDetialActivity: Class<*> = MessageDetailActivity::class.java

    var isNotificationEnabled = false
        private set

    var isNotificationSoundEnabled = false
        private set

    var isShowSignature = false
        private set

    var isShowColorText = false
        private set

    var isLeftHandMode = false
        private set

    var isShowBottomTab = false
        private set

    private var filterSubBoard = false

    private var sortByPostOrder = false

    private var avatarLoadStrategy: String = PreferenceKey.IMAGE_LOAD_ALWAYS

    private var imageLoadStrategy: String = PreferenceKey.IMAGE_LOAD_ALWAYS

    init {
        val sp = ContextUtils.getContext().getSharedPreferences(PreferenceKey.PERFERENCE, Context.MODE_PRIVATE)
        sp.registerOnSharedPreferenceChangeListener(this)
        for (key in KEYS) {
            onSharedPreferenceChanged(sp, key)
        }
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String?) {
        when (key) {
            PreferenceKey.NOTIFIACTION_SOUND -> isNotificationSoundEnabled = sp.getBoolean(key, true)
            PreferenceKey.ENABLE_NOTIFIACTION -> isNotificationEnabled = sp.getBoolean(key, true)
            PreferenceKey.SHOW_SIGNATURE -> isShowSignature = sp.getBoolean(key, false)
            PreferenceKey.SHOW_COLORTXT -> isShowColorText = sp.getBoolean(key, false)
            PreferenceKey.LEFT_HAND -> isLeftHandMode = sp.getBoolean(key, false)
            PreferenceKey.BOTTOM_TAB -> isShowBottomTab = sp.getBoolean(key, false)
            PreferenceKey.FILTER_SUB_BOARD -> filterSubBoard = sp.getBoolean(key, false)
            PreferenceKey.SORT_BY_POST -> sortByPostOrder = sp.getBoolean(key, false)
            PreferenceKey.KEY_LOAD_AVATAR_STRATEGY ->
                avatarLoadStrategy = sp.getString(key, avatarLoadStrategy) ?: avatarLoadStrategy
            PreferenceKey.KEY_LOAD_IMAGE_STRATEGY ->
                imageLoadStrategy = sp.getString(key, imageLoadStrategy) ?: imageLoadStrategy
        }
    }

    fun needSortByPostOrder(): Boolean = sortByPostOrder

    fun needFilterSubBoard(): Boolean = filterSubBoard

    var avatarSize: Int
        get() = try {
            PreferenceUtils.getData(PreferenceKey.KEY_AVATAR_SIZE, Constants.AVATAR_SIZE_DEFAULT)
        } catch (e: Exception) {
            avatarSize = Constants.AVATAR_SIZE_DEFAULT
            Constants.AVATAR_SIZE_DEFAULT
        }
        set(value) = PreferenceUtils.putData(PreferenceKey.KEY_AVATAR_SIZE, value)

    var emoticonSize: Int
        get() = PreferenceUtils.getData(PreferenceKey.KEY_EMOTICON_SIZE, Constants.EMOTICON_SIZE_DEFAULT)
        set(value) = PreferenceUtils.putData(PreferenceKey.KEY_EMOTICON_SIZE, value)

    val topicTitleSize: Float
        get() = PreferenceUtils.getData(PreferenceKey.KEY_TOPIC_TITLE_SIZE, Constants.TOPIC_TITLE_SIZE_DEFAULT).toFloat()

    fun setTopicTitleSize(size: Int) = PreferenceUtils.putData(PreferenceKey.KEY_TOPIC_TITLE_SIZE, size)

    var topicContentSize: Int
        get() = PreferenceUtils.getData(PreferenceKey.KEY_TOPIC_CONTENT_SIZE, Constants.TOPIC_CONTENT_SIZE_DEFAULT)
        set(value) = PreferenceUtils.putData(PreferenceKey.KEY_TOPIC_CONTENT_SIZE, value)

    var webViewTextZoom: Int
        get() = PreferenceUtils.getData(PreferenceKey.KEY_WEBVIEW_TEXT_ZOOM, Constants.WEBVIEW_DEFAULT_TEXT_ZOOM)
        set(value) = PreferenceUtils.putData(PreferenceKey.KEY_WEBVIEW_TEXT_ZOOM, value)

    val isAvatarLoadEnabled: Boolean
        get() = isLoadEnabled(avatarLoadStrategy)

    val isImageLoadEnabled: Boolean
        get() = isLoadEnabled(imageLoadStrategy)

    private fun isLoadEnabled(strategy: String): Boolean = when (strategy) {
        PreferenceKey.IMAGE_LOAD_ALWAYS -> true
        PreferenceKey.IMAGE_LOAD_NEVER -> false
        else -> DeviceUtils.isWifiConnected(ContextUtils.getContext())
    }

    @Deprecated("使用 topicContentSize", ReplaceWith("topicContentSize"))
    val webSize: Int
        get() = topicContentSize

    companion object {

        private val KEYS = listOf(
            PreferenceKey.NOTIFIACTION_SOUND,
            PreferenceKey.ENABLE_NOTIFIACTION,
            PreferenceKey.SHOW_SIGNATURE,
            PreferenceKey.SHOW_COLORTXT,
            PreferenceKey.LEFT_HAND,
            PreferenceKey.BOTTOM_TAB,
            PreferenceKey.FILTER_SUB_BOARD,
            PreferenceKey.SORT_BY_POST,
            PreferenceKey.KEY_LOAD_IMAGE_STRATEGY,
            PreferenceKey.KEY_LOAD_AVATAR_STRATEGY,
        )

        private val INSTANCE by lazy { PhoneConfiguration() }

        @JvmStatic
        fun getInstance(): PhoneConfiguration = INSTANCE
    }
}
