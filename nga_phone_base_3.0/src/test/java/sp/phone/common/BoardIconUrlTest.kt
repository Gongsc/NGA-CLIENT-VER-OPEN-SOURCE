package sp.phone.common

import org.junit.Assert.assertEquals
import org.junit.Test

/** 版块图标地址；img4.nga.178.com 已不可用，不能改回。 */
class BoardIconUrlTest {
    @Test fun boardIconsUseWorkingHttpsHost() {
        assertEquals("https://img4.nga.cn/ngabbs/nga_classic/f/app/552.png", String.format(ApiConstants.URL_BOARD_ICON, 552))
        assertEquals("https://img4.nga.cn/ngabbs/nga_classic/f/app/-7.png", String.format(ApiConstants.URL_BOARD_ICON, -7))
        assertEquals("https://img4.nga.cn/proxy/cache_attach/ficon/13011773v.png", String.format(ApiConstants.URL_BOARD_ICON_STID, 13011773))
    }
}
