package gov.anzong.androidnga.ui.compose

import org.junit.Assert.*
import org.junit.Test

class SearchAndFilterTest {
    private val boards = listOf(BoardTile(1, 0, "魔兽世界 [怀旧服]"), BoardTile(2, 0, "PC 游戏"), BoardTile(3, 0, "最终幻想"))

    @Test fun filterTreatsRegexCharactersAsLiteralInput() {
        assertEquals(listOf(boards[0]), filterBoards(boards, "["))
        assertTrue(filterBoards(boards, "(").isEmpty())
        assertTrue(filterBoards(boards, "*").isEmpty())
    }

    @Test fun filterTrimsWhitespaceAndIgnoresLatinCase() {
        assertEquals(listOf(boards[1]), filterBoards(boards, " pc "))
        assertEquals(boards, filterBoards(boards, "  "))
    }

    @Test fun repeatingAQueryMovesItToFrontWithoutDuplicates() {
        assertEquals(listOf("魔兽", "游戏"), updatedSearchHistory(listOf("游戏", "魔兽", "魔兽"), "魔兽"))
    }

    @Test fun blankQueryDoesNotEraseHistory() {
        assertEquals(listOf("游戏"), updatedSearchHistory(listOf("游戏"), "  "))
    }

    @Test fun forumAndCollectionWithSameIdHaveDistinctKeys() {
        assertNotEquals(BoardTile(1, 0, "版块").key, BoardTile(1, 1, "合集").key)
    }
}
