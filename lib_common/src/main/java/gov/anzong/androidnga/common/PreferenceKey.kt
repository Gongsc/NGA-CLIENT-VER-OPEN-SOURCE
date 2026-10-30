package gov.anzong.androidnga.common

/** 设置项键名；已有键名需保持不变，以兼容用户保存的设置。 */
object PreferenceKey {

    const val PREFERENCE_AVATAR = "avatar"

    const val PREFERENCE_SETTINGS = "perference"

    const val PERFERENCE = "perference"

    const val ENABLE_NOTIFIACTION = "enableNotification"

    const val NOTIFIACTION_SOUND = "notificationSound"

    const val NIGHT_MODE = "nightmode"

    const val REFRESH_AFTERPOST_SETTING_MODE = "refresh_after_post_setting_mode"

    const val SHOW_SIGNATURE = "showSignature"

    const val SHOW_COLORTXT = "showColortxt"

    const val USER_LIST = "userList"

    const val BLACK_LIST = ""

    const val FILTER_KEYWORDS_LIST = "filter_keywords"

    const val SHOW_ICON_MODE = "showiconmode"

    const val ADJUST_SIZE = "adjust_size"

    const val MATERIAL_THEME = "material_theme"

    const val BOTTOM_TAB = "bottom_tab"

    const val LEFT_HAND = "left_hand"

    const val HARDWARE_ACCELERATED = "hardware_accelerated"

    const val USER_ACTIVE_INDEX = "user_active_index"

    const val SORT_BY_POST = "sort_by_post"

    const val PREF_BLACK_LIST = "pref_black_list"

    const val PREF_USER = "pref_user"

    const val FILTER_SUB_BOARD = "filter_sub_board"

    const val KEY_TOPIC_HISTORY = "topic_history"

    const val KEY_REPLY_COUNT = "reply_count"

    const val KEY_SWIPE_BACK = "swipe_back"

    const val KEY_NGA_DOMAIN = "nga_domain"

    const val KEY_SEARCH_HISTORY_TOPIC = "search_history_topic"

    const val KEY_SEARCH_HISTORY_BOARD = "search_history_board"

    const val KEY_SEARCH_HISTORY_USER = "search_history_user"

    const val KEY_TOPIC_TITLE_SIZE = "topic_title_size"

    const val KEY_TOPIC_CONTENT_SIZE = "topic_content_size"

    const val KEY_AVATAR_SIZE = "avatar_size"

    const val KEY_EMOTICON_SIZE = "emoticon_size"

    const val KEY_USE_SOLID_COLOR_BG = "use_solid_color_bg"

    const val KEY_CHECK_UPGRADE_STATE = "key_check_upgrade_state"

    const val KEY_CHECK_UPGRADE_TIME = "key_check_upgrade_time"

    const val KEY_NIGHT_MODE_FOLLOW_SYSTEM = "key_night_mode_follow_system"

    const val VERSION_MAJOR_CODE = "version_major_code"

    const val VERSION_MIRROR_CODE = "version_mirror_code"

    const val VERSION_CODE = "version_code"

    const val PREVIOUS_VERSION_CODE = "previous_version_code"

    const val KEY_WEBVIEW_DATA_INDEX = "webview_data_index"

    const val KEY_CLEAR_CACHE = "key_clear_cache"

    const val KEY_WEBVIEW_TEXT_ZOOM = "key_webview_zoom_size"

    const val KEY_LOAD_AVATAR_STRATEGY = "pref_load_avatar_strategy"

    const val KEY_LOAD_IMAGE_STRATEGY = "pref_load_pic_strategy"

    const val IMAGE_LOAD_ALWAYS = "0"

    const val IMAGE_LOAD_NEVER = "1"

    const val IMAGE_LOAD_ONLY_WIFI = "2"

    @Deprecated("仅用于迁移旧设置")
    const val DOWNLOAD_AVATAR_NO_WIFI = "download_avatar_nowifi"

    @Deprecated("仅用于迁移旧设置")
    const val DOWNLOAD_IMG_NO_WIFI = "down_load_without_wifi"
}
