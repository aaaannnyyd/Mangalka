package mangalka.comments

import kotlinx.coroutines.runBlocking
import mangalka.comments.adapter.MangaDexCommentAdapter
import mangalka.comments.model.CommentFlag
import mangalka.comments.model.CommentNode
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MangaDexCommentAdapterTest {

    private val uuid = "af6667a4-ae4c-4baa-9283-a01f56d7f489"

    private fun http(replies: Int, pages: Map<Int, String>, requests: MutableList<String>) = object : CommentHttp {
        override suspend fun get(url: String, headers: Map<String, String>): HttpText {
            requests += url
            val body = when {
                url == "https://api.test/statistics/manga/$uuid" ->
                    """{"result":"ok","statistics":{"$uuid":{"comments":{"threadId":77,"repliesCount":$replies}}}}"""
                url.startsWith("https://forum.test/threads/77/page-") ->
                    pages.getValue(url.substringAfterLast("page-").toInt())
                else -> error("Unexpected URL: $url")
            }
            return HttpText(200, body, url)
        }
    }

    private val adapter = MangaDexCommentAdapter(apiBase = "https://api.test", forumBase = "https://forum.test")
    private val target = CommentTarget.Manga(sourceId = 1L, mangaUrl = "/manga/$uuid")

    @Test
    fun `skips the bot post and parses authors, quotes, spoilers and staff`() = runBlocking {
        val requests = mutableListOf<String>()
        val page = adapter.fetch(target, 1, CommentSort.OLDEST, http(2, mapOf(1 to THREAD_PAGE), requests))

        assertEquals(
            listOf("https://api.test/statistics/manga/$uuid", "https://forum.test/threads/77/page-1"),
            requests,
        )
        assertEquals(2, page.totalCount)
        assertFalse(page.hasNextPage)
        assertEquals(listOf("11", "12"), page.comments.map { it.id })

        val first = page.comments[0]
        assertEquals("Alice", first.author.name)
        assertEquals("https://forum.test/community/avatars/m/1.jpg", first.author.avatarUrl)
        assertEquals(1_791_667_677_000L, first.createdAt)
        assertTrue(first.body.any { it is CommentNode.Spoiler })
        assertTrue("great chapter" in first.plainText)

        val second = page.comments[1]
        assertTrue(CommentFlag.MODERATOR in second.flags)
        assertEquals("Staff", second.author.badge)
        val quote = second.body.filterIsInstance<CommentNode.Quote>().single()
        assertEquals("Alice", quote.author)
        assertFalse("said:" in second.plainText)
        assertFalse(second.body.any { it is CommentNode.Image })
        assertEquals(5, second.votes.up)
    }

    @Test
    fun `newest first starts from the last forum page`() = runBlocking {
        val requests = mutableListOf<String>()
        val pages =
            mapOf(1 to THREAD_PAGE, 2 to THREAD_PAGE.replace("post-11", "post-31").replace("post-12", "post-32"))
        val http = http(25, pages, requests)

        val newest = adapter.fetch(target, 1, CommentSort.NEWEST, http)
        assertEquals("https://forum.test/threads/77/page-2", requests.last())
        assertEquals(listOf("32", "31"), newest.comments.map { it.id })
        assertTrue(newest.hasNextPage)

        val older = adapter.fetch(target, 2, CommentSort.NEWEST, http)
        assertEquals("https://forum.test/threads/77/page-1", requests.last())
        assertFalse(older.hasNextPage)
    }

    @Test
    fun `title without a thread has no comments`() = runBlocking {
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>) =
                HttpText(200, """{"result":"ok","statistics":{"$uuid":{"comments":null}}}""", url)
        }
        assertTrue(adapter.fetch(target, 1, CommentSort.NEWEST, http).comments.isEmpty())
    }

    private companion object {
        val THREAD_PAGE = """
            <html><body>
            <article class="message message--post js-post is-first" data-author="MangaDex" data-content="post-10">
              <div class="message-body"><div class="bbWrapper"><a href="https://mangadex.org/manga/x"><img src="https://og/x"/></a></div></div>
            </article>
            <article class="message message--post js-post" data-author="Alice" data-content="post-11">
              <div class="message-avatar"><img src="/community/avatars/m/1.jpg"/></div>
              <h4 class="message-name"><a href="/members/alice.1/" class="username" data-user-id="1"><span>Alice</span></a></h4>
              <time class="u-dt" data-time="1791667677">Oct 10</time>
              <div class="message-body"><div class="bbWrapper">great chapter
                <div class="bbCodeBlock bbCodeBlock--spoiler"><button class="bbCodeSpoiler-button">Spoiler</button>
                  <div class="bbCodeSpoiler-content">he dies</div></div></div></div>
            </article>
            <article class="message message--post js-post" data-author="Mod" data-content="post-12">
              <h4 class="message-name"><a href="/members/mod.2/" class="username" data-user-id="2"><span class="username--staff username--moderator">Mod</span></a></h4>
              <div class="userBanner message-userBanner"><strong>Staff</strong></div>
              <time class="u-dt" data-time="1791667700">Oct 10</time>
              <div class="message-body"><div class="bbWrapper">
                <blockquote class="bbCodeBlock bbCodeBlock--expandable bbCodeBlock--quote" data-quote="Alice">
                  <div class="bbCodeBlock-title">Alice said:</div>
                  <div class="bbCodeBlock-content"><div class="bbCodeBlock-expandContent">great chapter</div>
                  <div class="bbCodeBlock-expandLink">Click to expand...</div></div>
                </blockquote>
                <blockquote class="bbCodeBlock bbCodeBlock--quote" data-quote="MangaDex"><div class="bbCodeBlock-content"><img src="https://og/x"/></div></blockquote>agreed</div></div>
              <footer><div class="reactionsBar"><a class="reactionsBar-link"><bdi>A</bdi>, <bdi>B</bdi> and 3 others</a></div></footer>
            </article>
            </body></html>
        """.trimIndent()
    }
}
