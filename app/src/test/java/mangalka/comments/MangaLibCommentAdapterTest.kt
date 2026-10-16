package mangalka.comments

import kotlinx.coroutines.runBlocking
import mangalka.comments.adapter.MangaLibCommentAdapter
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MangaLibCommentAdapterTest {

    @Test
    fun `SlashLib is registered as lib group site`() {
        val adapter = MangaLibCommentAdapter()

        assertTrue(adapter.supports(2730544188738947015L, "https://slashlib.me"))
        assertTrue(adapter.supports(999L, "https://slashlib.me"))
        assertFalse(adapter.supports(999L, "https://example.org"))
    }

    @Test
    fun `chapter id resolves from extension query url and authenticated api mirror`() = runBlocking {
        val requests = mutableListOf<String>()
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                requests += url
                val body = when (url) {
                    "https://api.cdnlibs.org/api/manga/7580--sample/chapters" -> """{"data":[]}"""
                    "https://api2.mangalib.me/api/manga/7580--sample/chapters" -> CHAPTERS
                    "https://api.cdnlibs.org/api/comments?post_id=321&post_type=chapter&page=1" -> COMMENTS
                    else -> error("Unexpected URL: $url")
                }
                return HttpText(200, body, url)
            }
        }
        val adapter = MangaLibCommentAdapter()
        val target = CommentTarget.Chapter(
            sourceId = 6111047689498497237L,
            mangaUrl = "/7580--sample",
            chapterUrl = "/7580--sample/chapter?&volume=1&number=86",
            absoluteUrl = "https://mangalib.me/ru/7580--sample/read/v1/c86",
            chapterName = "Том 1. Глава 86",
            chapterNumber = 86f,
        )

        val page = adapter.fetch(target, page = 1, sort = CommentSort.NEWEST, http = http)

        assertEquals(1, page.comments.size)
        assertEquals("900", page.comments.single().id)
        assertEquals("chapter comment", page.comments.single().plainText)
        assertEquals(
            listOf(
                "https://api.cdnlibs.org/api/manga/7580--sample/chapters",
                "https://api2.mangalib.me/api/manga/7580--sample/chapters",
                "https://api.cdnlibs.org/api/comments?post_id=321&post_type=chapter&page=1",
            ),
            requests,
        )
    }

    @Test
    fun `chapter id respects exact branch id from extension url`() = runBlocking {
        val requests = mutableListOf<String>()
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                requests += url
                val body = when (url) {
                    "https://api.cdnlibs.org/api/manga/7580--sample/chapters" -> CHAPTERS_WITH_BRANCHES
                    "https://api.cdnlibs.org/api/comments?post_id=222&post_type=chapter&page=1" -> COMMENTS
                    else -> error("Unexpected URL: $url")
                }
                return HttpText(200, body, url)
            }
        }
        val target = CommentTarget.Chapter(
            sourceId = 6111047689498497237L,
            mangaUrl = "/7580--sample",
            chapterUrl = "/7580--sample/chapter?branch_id=22&volume=1&number=86",
            absoluteUrl = "https://mangalib.me/ru/7580--sample/read/v1/c86?bid=22",
            chapterName = "Том 1. Глава 86",
            chapterNumber = 86f,
        )

        val page = MangaLibCommentAdapter().fetch(target, 1, CommentSort.NEWEST, http)

        assertEquals("900", page.comments.single().id)
        assertEquals(
            listOf(
                "https://api.cdnlibs.org/api/manga/7580--sample/chapters",
                "https://api.cdnlibs.org/api/comments?post_id=222&post_type=chapter&page=1",
            ),
            requests,
        )
    }

    private companion object {
        const val CHAPTERS = """
            {
              "data": [
                {"id": 321, "number": "86", "volume": "1"}
              ]
            }
        """

        const val CHAPTERS_WITH_BRANCHES = """
            {
              "data": [
                {
                  "id": 111,
                  "number": "86",
                  "volume": "1",
                  "branches": [{"branch_id": 11}]
                },
                {
                  "id": 222,
                  "number": "86",
                  "volume": "1",
                  "branches": [{"branch_id": 22}]
                }
              ]
            }
        """

        const val COMMENTS = """
            {
              "data": {
                "root": [
                  {
                    "id": 900,
                    "parent_comment": null,
                    "comment": "chapter comment",
                    "created_at_ts": 1780000000000,
                    "user": {"id": 12, "username": "Reader"},
                    "votes": {"up": 1, "down": 0}
                  }
                ],
                "replies": []
              },
              "meta": {"has_next_page": false, "page": 1}
            }
        """
    }
}
