package mangalka.comments

import kotlinx.coroutines.runBlocking
import mangalka.comments.adapter.HenChanCommentAdapter
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HenChanCommentAdapterTest {

    @Test
    fun `HenChan is exact source adapter and supports chapter comments`() {
        val adapter = HenChanCommentAdapter()

        assertTrue(adapter.supports(5504588601186153612L, "https://example.test"))
        assertTrue(adapter.supports(1L, "https://xxl.hentaichan.live"))
        assertTrue(adapter.supports(1L, "https://manga.h-chan.me"))
        assertFalse(adapter.supports(1L, "https://example.test"))
        assertTrue(adapter.supportsChapterComments)
    }

    @Test
    fun `HenChan DLE page parses author body date avatar score parent and exact next url`() = runBlocking {
        val requests = mutableListOf<String>()
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                requests += url
                val body = when (url) {
                    "https://xxl.hentaichan.live/online/sample.html" -> PAGE_ONE
                    "https://xxl.hentaichan.live/online/sample.html?cstart=2" -> PAGE_TWO
                    else -> error("Unexpected URL: $url")
                }
                return HttpText(200, body, url)
            }
        }
        val adapter = HenChanCommentAdapter()
        val target = CommentTarget.Chapter(
            sourceId = 5504588601186153612L,
            mangaUrl = "/manga/sample.html",
            chapterUrl = "/manga/sample.html",
            absoluteUrl = "https://xxl.hentaichan.live/manga/sample.html",
            chapterName = "Sample",
            chapterNumber = 1f,
        )

        val first = adapter.fetch(target, page = 1, sort = CommentSort.NEWEST, http = http)
        val second = adapter.fetch(target, page = 2, sort = CommentSort.NEWEST, http = http)

        assertEquals("henchan", first.providerId)
        assertTrue(first.hasNextPage)
        assertEquals(1, first.comments.size)

        val root = first.comments.single()
        assertEquals("101", root.id)
        assertEquals("Alice", root.author.name)
        assertEquals("https://xxl.hentaichan.live/uploads/fotos/alice.jpg", root.author.avatarUrl)
        assertNotNull(root.createdAt)
        assertEquals("root body", root.plainText)
        assertNull(root.votes.up)
        assertNull(root.votes.down)
        assertEquals(7, root.votes.score)

        assertEquals(1, root.children.size)
        val reply = root.children.single()
        assertEquals("102", reply.id)
        assertEquals("101", reply.parentId)
        assertEquals("Bob", reply.author.name)
        assertEquals("reply body", reply.plainText)

        assertFalse(second.hasNextPage)
        assertEquals("103", second.comments.single().id)
        assertEquals(
            listOf(
                "https://xxl.hentaichan.live/online/sample.html",
                "https://xxl.hentaichan.live/online/sample.html?cstart=2",
            ),
            requests,
        )
    }

    private companion object {
        const val PAGE_ONE = """
            <html><body>
              <div id="dle-comments-list">
                <div class="comment" id="comment-id-101">
                  <div class="com_info">
                    <div class="avatar"><span class="cover" style="background-image: url('/uploads/fotos/alice.jpg')"></span></div>
                    <div class="com_user"><b class="name">Alice</b><span class="grey">от 26 августа 2026 12:34</span></div>
                    <div class="meta"><div class="rate_like-dislike"><i class="icon icon-like"></i>7<i class="icon icon-dislike"></i></div></div>
                  </div>
                  <div class="com_content"><div class="text">root <b>body</b></div></div>
                </div>
                <div class="comment" id="comment-id-102" data-parent="101">
                  <div class="com_info"><div class="com_user"><b class="name">Bob</b><span class="grey">Сегодня, 10:00</span></div></div>
                  <div class="com_content"><div class="text">reply body</div></div>
                </div>
              </div>
              <div class="navigation"><a class="next" href="?cstart=2">Вперед</a></div>
            </body></html>
        """

        const val PAGE_TWO = """
            <html><body>
              <div id="dle-comments-list">
                <div class="comment" id="comment-id-103">
                  <div class="com_info"><div class="com_user"><b class="name">Carol</b><span class="grey">Вчера, 23:11</span></div></div>
                  <div class="com_content"><div class="text">page two</div></div>
                </div>
              </div>
            </body></html>
        """
    }
}
