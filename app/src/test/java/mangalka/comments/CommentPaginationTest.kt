package mangalka.comments

import kotlinx.coroutines.runBlocking
import mangalka.comments.adapter.ComixCommentAdapter
import mangalka.comments.model.CommentAuthor
import mangalka.comments.model.CommentNode
import mangalka.comments.model.CommentPage
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import mangalka.comments.model.UnifiedComment
import mangalka.comments.util.countForPage
import mangalka.comments.util.filterForPage
import mangalka.comments.util.mergeCommentPages
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommentPaginationTest {

    @Test
    fun `mergeCommentPages keeps existing and new replies under repeated root`() {
        val rootPage1 = comment("1", text = "root old").copy(
            children = listOf(comment("2", parentId = "1", text = "reply 1", depth = 1)),
        )
        val rootPage2 = comment("1", text = "root updated").copy(
            children = listOf(comment("3", parentId = "1", text = "reply 2", depth = 1)),
        )

        val merged = mergeCommentPages(
            current = CommentPage(
                comments = listOf(rootPage1),
                page = 1,
                hasNextPage = true,
                totalCount = 3,
                providerId = "test",
            ),
            next = CommentPage(
                comments = listOf(rootPage2),
                page = 2,
                hasNextPage = false,
                totalCount = 3,
                providerId = "test",
            ),
        )

        assertEquals(2, merged.page)
        assertFalse(merged.hasNextPage)
        assertEquals(1, merged.comments.size)
        assertEquals("root updated", merged.comments.single().plainText)
        assertEquals(listOf("2", "3"), merged.comments.single().children.map { it.id })
        assertEquals(listOf(1, 1), merged.comments.single().children.map { it.depth })
    }

    @Test
    fun `page filter keeps parent context and counts only exact page comments`() {
        val tree = listOf(
            comment("1", text = "root", pageNumber = 1).copy(
                children = listOf(
                    comment("2", parentId = "1", text = "page two reply", depth = 1, pageNumber = 2),
                    comment("3", parentId = "1", text = "page one reply", depth = 1, pageNumber = 1),
                ),
            ),
        )

        val filtered = tree.filterForPage(2)

        assertEquals(1, filtered.size)
        assertEquals("1", filtered.single().id)
        assertEquals(listOf("2"), filtered.single().children.map { it.id })
        assertEquals(1, tree.countForPage(2))
    }

    @Test
    fun `Comix page two uses cursor returned by page one`() = runBlocking {
        val requests = mutableListOf<String>()
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                requests += url
                val body = when {
                    url == "https://example.test/title/abc-title" -> {
                        """<script id="initial-data">{"queries":{"[\"manga\",\"detail\",\"abc\"]":{"id":123}}}</script>"""
                    }
                    url.startsWith("https://example.test/api/v1/threads/lookup") -> {
                        """{"result":{"thread":{"id":77,"commentCount":2}}}"""
                    }
                    url == "https://example.test/api/v1/threads/77/comments?sort=newest" -> {
                        """{"result":{"items":[{"id":1,"contentHtml":"one","replies":[]}],"cursor":"cursor one"}}"""
                    }
                    url == "https://example.test/api/v1/threads/77/comments?sort=newest&cursor=cursor+one" -> {
                        """{"result":{"items":[{"id":2,"contentHtml":"two","replies":[]}],"cursor":""}}"""
                    }
                    else -> error("Unexpected URL: $url")
                }
                return HttpText(code = 200, body = body, finalUrl = url)
            }
        }
        val adapter = ComixCommentAdapter(
            sourceIds = setOf(1L),
            domains = listOf("example.test"),
            apiBase = "https://example.test/api/v1",
            siteBase = "https://example.test",
        )
        val target = CommentTarget.Manga(
            sourceId = 1L,
            mangaUrl = "/abc-title",
            absoluteUrl = "https://example.test/title/abc-title",
            title = "Test",
        )

        val first = adapter.fetch(target, page = 1, sort = CommentSort.NEWEST, http = http)
        val second = adapter.fetch(target, page = 2, sort = CommentSort.NEWEST, http = http)

        assertTrue(first.hasNextPage)
        assertEquals("1", first.comments.single().id)
        assertFalse(second.hasNextPage)
        assertEquals("2", second.comments.single().id)
        assertTrue(requests.any { it.endsWith("cursor=cursor+one") })
    }

    @Test
    fun `Comix cannot jump to page two without cursor`() = runBlocking {
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                val body = when {
                    url == "https://example.test/title/abc-title" -> {
                        """<script id="initial-data">{"queries":{"[\"manga\",\"detail\",\"abc\"]":{"id":123}}}</script>"""
                    }
                    url.startsWith("https://example.test/api/v1/threads/lookup") -> {
                        """{"result":{"thread":{"id":77,"commentCount":2}}}"""
                    }
                    else -> error("Comment list must not be requested without a cursor: $url")
                }
                return HttpText(code = 200, body = body, finalUrl = url)
            }
        }
        val adapter = ComixCommentAdapter(
            sourceIds = setOf(1L),
            domains = listOf("example.test"),
            apiBase = "https://example.test/api/v1",
            siteBase = "https://example.test",
        )
        val target = CommentTarget.Manga(
            sourceId = 1L,
            mangaUrl = "/abc-title",
            absoluteUrl = "https://example.test/title/abc-title",
        )

        val page = adapter.fetch(target, page = 2, sort = CommentSort.NEWEST, http = http)

        assertTrue(page.comments.isEmpty())
        assertFalse(page.hasNextPage)
    }

    private fun comment(
        id: String,
        parentId: String? = null,
        text: String,
        depth: Int = 0,
        pageNumber: Int? = null,
    ) = UnifiedComment(
        id = id,
        parentId = parentId,
        author = CommentAuthor("user-$id"),
        body = listOf(CommentNode.Text(text)),
        depth = depth,
        pageNumber = pageNumber,
    )
}
