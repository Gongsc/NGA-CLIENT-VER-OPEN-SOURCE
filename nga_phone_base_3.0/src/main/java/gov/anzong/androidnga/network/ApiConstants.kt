package gov.anzong.androidnga.network

/** 论坛接口中的地址模板、提醒类型与标题样式位。 */
object ApiConstants {

    // 版块图标：img4.nga.178.com 已不可用（http 返回 502，https 握手被断开），改用 img4.nga.cn。
    const val URL_BOARD_ICON = "https://img4.nga.cn/ngabbs/nga_classic/f/app/%s.png"

    const val URL_BOARD_ICON_STID = "https://img4.nga.cn/proxy/cache_attach/ficon/%sv.png"

    const val NGA_NOTIFICATION_TYPE_TOPIC_REPLY = 1

    const val NGA_NOTIFICATION_TYPE_REPLY_REPLY = 2

    const val NGA_NOTIFICATION_TYPE_TOPIC_COMMENT = 3

    const val NGA_NOTIFICATION_TYPE_REPLY_COMMENT = 4

    const val NGA_NOTIFICATION_TYPE_TOPIC_AT = 7

    const val NGA_NOTIFICATION_TYPE_REPLY_AT = 8

    const val MASK_FONT_RED = 1

    const val MASK_FONT_BLUE = 2

    const val MASK_FONT_GREEN = 4

    const val MASK_FONT_ORANGE = 8

    const val MASK_FONT_SILVER = 16

    const val MASK_FONT_BOLD = 32

    const val MASK_FONT_ITALIC = 64

    const val MASK_FONT_UNDERLINE = 128

    // 主题被锁定 2^10
    const val MASK_TYPE_LOCK = 1024

    // 主题中有附件 2^13
    const val MASK_TYPE_ATTACHMENT = 8192

    // 合集 2^15
    const val MASK_TYPE_ASSEMBLE = 32768
}
