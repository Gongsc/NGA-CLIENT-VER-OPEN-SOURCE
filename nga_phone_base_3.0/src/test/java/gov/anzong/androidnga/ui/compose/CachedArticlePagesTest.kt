package gov.anzong.androidnga.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CachedArticlePagesTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun pagesSortNumericallyAndSkipTopicInfo() {
        val dir = folder.newFolder("123")
        listOf("2.json", "10.json", "1.json", "123.json", "notes.txt").forEach { dir.resolve(it).writeText("{}") }
        assertEquals(listOf(1, 2, 10), cachedArticlePages(dir, "123"))
    }

    @Test fun missingDirectoryHasNoPages() {
        assertEquals(emptyList<Int>(), cachedArticlePages(folder.root.resolve("missing"), "1"))
    }
}
