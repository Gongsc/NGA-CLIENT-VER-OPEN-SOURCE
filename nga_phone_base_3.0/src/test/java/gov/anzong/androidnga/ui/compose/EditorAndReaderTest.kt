package gov.anzong.androidnga.ui.compose

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.*
import org.junit.Test

class EditorAndReaderTest {
    @Test fun insertionReplacesSelectionAndPositionsCursor() {
        val result = insertEditorText(TextFieldValue("abcdef", TextRange(2, 4)), "[b][/b]", 3)
        assertEquals("ab[b][/b]ef", result.text)
        assertEquals(TextRange(5), result.selection)
    }
    @Test fun formattingPreservesSelectedTextAndClosesParameterizedTags() {
        val result = formatEditorSelection(TextFieldValue("abc", TextRange(0, 3)), "color=red")
        assertEquals("[color=red]abc[/color]", result.text)
        assertEquals(TextRange(14), result.selection)
    }
    @Test fun emptySelectionPlacesCursorBetweenTags() {
        val result = formatEditorSelection(TextFieldValue("文字", TextRange(2)), "quote")
        assertEquals("文字[quote][/quote]", result.text)
        assertEquals(TextRange(9), result.selection)
    }
    @Test fun pageCountHandlesBoundariesAndIntegerOverflow() {
        assertEquals(1, articlePageCount(0))
        assertEquals(1, articlePageCount(20))
        assertEquals(2, articlePageCount(21))
        assertEquals(107374183, articlePageCount(Int.MAX_VALUE))
    }
    @Test fun floorJumpUsesZeroBasedFloorsAndValidatesBounds() {
        assertEquals(ReaderPosition(0, 0), readerPosition("0", true, 2, 40))
        assertEquals(ReaderPosition(1, 20), readerPosition("20", true, 2, 40))
        assertNull(readerPosition("40", true, 2, 40))
        assertNull(readerPosition("-1", true, 2, 40))
        assertNull(readerPosition("9999999999999999", true, 2, 40))
    }
    @Test fun pageJumpUsesOneBasedInput() {
        assertEquals(ReaderPosition(1), readerPosition(" 2 ", false, 3, 60))
        assertNull(readerPosition("0", false, 3, 60))
        assertNull(readerPosition("4", false, 3, 60))
    }
}
