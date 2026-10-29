package gov.anzong.androidnga.ui.compose
import org.junit.Assert.*
import org.junit.Test
class ArticleTypographyTest {
    @Test fun bothRenderersShareReadingZoom() {
        assertEquals(16.8f, articleFontSize(24,70),0.001f)
        assertEquals(21.6f, articleFontSize(24,90),0.001f)
        assertEquals(24f, articleFontSize(24,100),0.001f)
    }

    @Test fun avatarSettingMapsToDisplaySizeAroundDefault() {
        assertEquals(32f, articleAvatarSize(104).value, 0.001f)
        assertEquals(40f, articleAvatarSize(130).value, 0.001f)
        assertEquals(20f, articleAvatarSize(1).value, 0.001f)
        assertEquals(44f, articleAvatarSize(999).value, 0.001f)
    }
}
