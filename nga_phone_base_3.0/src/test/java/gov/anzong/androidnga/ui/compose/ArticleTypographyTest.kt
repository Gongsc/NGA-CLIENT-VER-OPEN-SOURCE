package gov.anzong.androidnga.ui.compose
import org.junit.Assert.*
import org.junit.Test
class ArticleTypographyTest {
    @Test fun bothRenderersShareReadingZoom() {
        assertEquals(16.8f, articleFontSize(24,70),0.001f)
        assertEquals(21.6f, articleFontSize(24,90),0.001f)
        assertEquals(24f, articleFontSize(24,100),0.001f)
    }
}
