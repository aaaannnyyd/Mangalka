package mangalka.comments

import kotlinx.coroutines.runBlocking
import mangalka.comments.adapter.ReMangaCommentAdapter
import mangalka.comments.model.CommentNode
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReMangaCommentAdapterTest {

    @Test
    fun `ReManga hydrates reply tree and normalizes rich comment data`() = runBlocking {
        val requests = mutableListOf<String>()
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                requests += url
                val body = when (url) {
                    "https://example.test/api/v2/titles/test-title/" -> TITLE_JSON
                    "https://example.test/api/v2/activity/comments/?title_id=10&page=1&ordering=-score" ->
                        ROOT_COMMENTS_V2_JSON
                    "https://example.test/api/v2/activity/comments/?title_id=10&page=1&ordering=id&reply_to=1" ->
                        REPLY_LEVEL_1_V2_JSON
                    "https://example.test/api/v2/activity/comments/?title_id=10&page=1&ordering=id&reply_to=3" ->
                        REPLY_LEVEL_2_V2_JSON
                    else -> error("Unexpected URL: $url")
                }
                return HttpText(200, body, url)
            }
        }
        val adapter = adapter()
        val target = CommentTarget.Manga(
            sourceId = 44L,
            mangaUrl = "/manga/test-title/main",
            absoluteUrl = "https://example.test/manga/test-title/main",
            title = "Test",
        )

        val page = adapter.fetch(target, page = 1, sort = CommentSort.TOP, http = http)

        assertEquals(4, page.totalCount)
        assertFalse(page.hasNextPage)
        assertEquals(2, page.comments.size)

        val root = page.comments.first()
        assertEquals("1", root.id)
        assertEquals("Hello world", root.plainText)
        assertEquals("Alice", root.author.name)
        assertEquals("https://example.test/media/alice.webp", root.author.avatarUrl)
        assertEquals("Veteran", root.author.badge)
        assertEquals(5, root.votes.score)
        assertNotNull(root.createdAt)
        assertEquals(1, root.children.size)

        val child = root.children.single()
        assertEquals("3", child.id)
        assertEquals("1", child.parentId)
        assertEquals(1, child.depth)
        assertEquals(1, child.children.size)

        val grandchild = child.children.single()
        assertEquals("4", grandchild.id)
        assertEquals("3", grandchild.parentId)
        assertEquals(2, grandchild.depth)

        val spoiler = page.comments[1]
        assertTrue(spoiler.hasSpoiler)
        assertTrue(spoiler.body.single() is CommentNode.Spoiler)
        assertEquals(-2, spoiler.votes.score)
        assertTrue(requests.any { it.endsWith("ordering=-score") })
    }

    @Test
    fun `ReManga resolves chapter id from v2 branch list`() = runBlocking {
        val requests = mutableListOf<String>()
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                requests += url
                val body = when (url) {
                    "https://example.test/api/v2/titles/test-title/" -> TITLE_JSON
                    "https://example.test/api/v2/titles/chapters/?branch_id=100&ordering=-index&page=1&count=100" ->
                        CHAPTERS_JSON
                    "https://example.test/api/v2/activity/comments/?chapter_id=77&page=1&ordering=-id" ->
                        CHAPTER_COMMENTS_V2_JSON
                    else -> error("Unexpected URL: $url")
                }
                return HttpText(200, body, url)
            }
        }
        val adapter = adapter()
        val target = CommentTarget.Chapter(
            sourceId = 44L,
            mangaUrl = "/manga/test-title/main",
            chapterUrl = "/manga/test-title/chapter/1",
            absoluteUrl = "https://example.test/manga/test-title/chapter/1",
            chapterName = "Chapter 1",
            chapterNumber = 1f,
        )

        val page = adapter.fetch(target, page = 1, sort = CommentSort.NEWEST, http = http)

        assertEquals(1, page.comments.size)
        assertEquals("91", page.comments.single().id)
        assertEquals("chapter comment", page.comments.single().plainText)
        assertTrue(requests.any { it.contains("branch_id=100") })
        assertTrue(requests.any { it.contains("chapter_id=77") })
    }

    @Test
    fun `ReManga uses v115 ch id directly without title or branch lookup`() = runBlocking {
        val requests = mutableListOf<String>()
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                requests += url
                return when (url) {
                    "https://example.test/api/v2/activity/comments/?chapter_id=777&page=1&ordering=-id" ->
                        HttpText(200, CHAPTER_COMMENTS_V2_JSON, url)
                    else -> error("Unexpected URL: $url")
                }
            }
        }

        val page = adapter().fetch(
            CommentTarget.Chapter(
                sourceId = 44L,
                mangaUrl = "/manga/test-title/main",
                chapterUrl = "/manga/test-title/main/ch777",
                absoluteUrl = "https://example.test/manga/test-title/main/ch777",
                chapterName = "Chapter 1",
                chapterNumber = 1f,
            ),
            page = 1,
            sort = CommentSort.NEWEST,
            http = http,
        )

        assertEquals(1, page.comments.size)
        assertEquals("91", page.comments.single().id)
        assertEquals(
            listOf("https://example.test/api/v2/activity/comments/?chapter_id=777&page=1&ordering=-id"),
            requests,
        )
    }

    @Test
    fun `ReManga falls back to legacy comments endpoint`() = runBlocking {
        val requests = mutableListOf<String>()
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                requests += url
                return when (url) {
                    "https://example.test/api/v2/titles/test-title/" -> HttpText(200, TITLE_JSON, url)
                    "https://example.test/api/v2/activity/comments/?title_id=10&page=1&ordering=-id" ->
                        HttpText(404, "", url)
                    "https://example.test/api/activity/comments?title_id=10&page=1&ordering=-id" ->
                        HttpText(200, LEGACY_COMMENTS_JSON, url)
                    else -> error("Unexpected URL: $url")
                }
            }
        }
        val page = adapter().fetch(
            CommentTarget.Manga(
                sourceId = 44L,
                mangaUrl = "/manga/test-title/main",
                absoluteUrl = "https://example.test/manga/test-title/main",
            ),
            page = 1,
            sort = CommentSort.NEWEST,
            http = http,
        )

        assertEquals("55", page.comments.single().id)
        assertEquals("legacy", page.comments.single().plainText)
        assertTrue(requests.any { it.contains("/v2/activity/comments/") })
        assertTrue(requests.any { it.contains("/activity/comments?title_id=10") })
    }

    @Test
    fun `ReManga propagates root comment outage instead of pretending thread is empty`() {
        val failure = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException::class.java) {
            runBlocking {
                val http = object : CommentHttp {
                    override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                        return when (url) {
                            "https://example.test/api/v2/titles/test-title/" -> HttpText(200, TITLE_JSON, url)
                            else -> throw java.io.IOException("offline")
                        }
                    }
                }
                adapter().fetch(
                    CommentTarget.Manga(
                        sourceId = 44L,
                        mangaUrl = "/manga/test-title/main",
                        absoluteUrl = "https://example.test/manga/test-title/main",
                    ),
                    page = 1,
                    sort = CommentSort.NEWEST,
                    http = http,
                )
            }
        }

        assertTrue(failure.message.orEmpty().contains("ReManga comments unavailable"))
    }

    @Test
    fun `ReManga support is domain based for debug and release extensions`() {
        val adapter = ReMangaCommentAdapter()
        assertTrue(adapter.supports(1L, "https://remanga.org"))
        assertTrue(adapter.supports(999L, "https://api.remanga.org"))
        assertFalse(adapter.supports(1L, "https://example.org"))
    }

    private fun adapter() = ReMangaCommentAdapter(
        domains = listOf("example.test"),
        apiRoot = "https://example.test/api",
        siteBase = "https://example.test",
    )

    private companion object {
        const val TITLE_JSON = """
            {
              "id": 10,
              "dir": "test-title",
              "count_comments": 4,
              "branches": [{"id": 100}]
            }
        """

        const val ROOT_COMMENTS_V2_JSON = """
            [
                {
                  "id": 1,
                  "text": "&lt;p&gt;Hello &lt;strong&gt;world&lt;/strong&gt;&lt;/p&gt;",
                  "count_replies": 1,
                  "date": 1000,
                  "score": 5,
                  "is_spoiler": false,
                  "is_pinned": true,
                  "rank": "Veteran",
                  "user": {
                    "id": 11,
                    "username": "Alice",
                    "avatar": {"high": "/media/alice.webp"},
                    "is_premium": false
                  }
                },
                {
                  "id": 2,
                  "text": "&lt;p&gt;secret&lt;/p&gt;",
                  "count_replies": 0,
                  "date": 2000,
                  "score": -2,
                  "is_spoiler": true,
                  "is_pinned": false,
                  "rank": "",
                  "user": {"id": 12, "username": "Bob", "avatar": {}, "is_premium": true}
                }
            ]
        """

        const val REPLY_LEVEL_1_V2_JSON = """
            [{
                "id": 3,
                "text": "reply",
                "count_replies": 1,
                "date": 500,
                "score": 1,
                "is_spoiler": false,
                "is_pinned": false,
                "rank": "",
                "user": {"id": 13, "username": "Carol", "avatar": {}, "is_premium": false}
              }]
        """

        const val REPLY_LEVEL_2_V2_JSON = """
            [{
                "id": 4,
                "text": "deep reply",
                "count_replies": 0,
                "date": 100,
                "score": 0,
                "is_spoiler": false,
                "is_pinned": false,
                "rank": "",
                "user": {"id": 14, "username": "Dan", "avatar": {}, "is_premium": false}
              }]
        """

        const val CHAPTERS_JSON = """
            {
              "next": null,
              "previous": null,
              "results": [
                {"id": 78, "chapter": "2", "name": "Second"},
                {"id": 77, "chapter": "1", "name": "First"}
              ]
            }
        """

        const val CHAPTER_COMMENTS_V2_JSON = """
            [{
              "id": 91,
              "text": "chapter comment",
              "count_replies": 0,
              "date": 50,
              "score": 3,
              "is_spoiler": false,
              "is_pinned": false,
              "rank": "",
              "user": {"id": 20, "username": "Reader", "avatar": {}, "is_premium": false}
            }]
        """

        const val LEGACY_COMMENTS_JSON = """
            {
              "content": [{
                "id": 55,
                "text": "legacy",
                "count_replies": 0,
                "date": 10,
                "score": 0,
                "is_spoiler": false,
                "is_pinned": false,
                "rank": "",
                "user": {"id": 21, "username": "Legacy", "avatar": {}, "is_premium": false}
              }],
              "props": {"page": 1}
            }
        """
    }
}
