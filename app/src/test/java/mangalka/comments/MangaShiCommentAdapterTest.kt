package mangalka.comments

import kotlinx.coroutines.runBlocking
import mangalka.comments.adapter.MangaShiCommentAdapter
import mangalka.comments.model.CommentNode
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MangaShiCommentAdapterTest {

    @Test
    fun `MangaShi parses nested title comments spoiler author votes and exact htmx pagination`() = runBlocking {
        val requests = mutableListOf<String>()
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                requests += url
                val body = when (url) {
                    "https://example.test/manga/sample/?sort=new" -> PAGE_ONE
                    "https://example.test/comments/more/?manga_id=2321&comments_page=2&sort=new" -> PAGE_TWO
                    else -> error("Unexpected URL: $url")
                }
                return HttpText(200, body, url)
            }
        }
        val adapter = MangaShiCommentAdapter(domains = listOf("example.test"))
        val target = CommentTarget.Manga(
            sourceId = 7L,
            mangaUrl = "/manga/sample/",
            absoluteUrl = "https://example.test/manga/sample/",
            title = "Sample",
        )

        val first = adapter.fetch(target, page = 1, sort = CommentSort.NEWEST, http = http)
        val second = adapter.fetch(target, page = 2, sort = CommentSort.NEWEST, http = http)

        assertEquals(49, first.totalCount)
        assertTrue(first.hasNextPage)
        assertEquals(1, first.comments.size)

        val root = first.comments.single()
        assertEquals("10", root.id)
        assertEquals("Alice", root.author.name)
        assertEquals("alice", root.author.id)
        assertEquals("Ветеран", root.author.badge)
        assertEquals("https://example.test/media/alice.jpg", root.author.avatarUrl)
        assertEquals(4, root.votes.up)
        assertEquals(1, root.votes.down)
        assertNotNull(root.createdAt)
        assertTrue(root.hasSpoiler)
        assertTrue(root.body.single() is CommentNode.Spoiler)
        assertEquals("secret text", root.plainText)

        assertEquals(1, root.children.size)
        val reply = root.children.single()
        assertEquals("11", reply.id)
        assertEquals("10", reply.parentId)
        assertEquals(1, reply.depth)
        assertEquals("reply body", reply.plainText)

        assertFalse(second.hasNextPage)
        assertEquals(49, second.totalCount)
        assertEquals("12", second.comments.single().id)
        assertTrue(requests.contains("https://example.test/comments/more/?manga_id=2321&comments_page=2&sort=new"))
    }

    @Test
    fun `MangaShi supports chapter comments and maps website sorts`() = runBlocking {
        val requests = mutableListOf<String>()
        val http = object : CommentHttp {
            override suspend fun get(url: String, headers: Map<String, String>): HttpText {
                requests += url
                return HttpText(200, CHAPTER_PAGE, url)
            }
        }
        val adapter = MangaShiCommentAdapter(domains = listOf("example.test"))
        val target = CommentTarget.Chapter(
            sourceId = 7L,
            mangaUrl = "/manga/sample/",
            chapterUrl = "/manga/sample/glava-86/",
            absoluteUrl = "https://example.test/manga/sample/glava-86/",
            chapterName = "Глава 86",
            chapterNumber = 86f,
        )

        val page = adapter.fetch(target, page = 1, sort = CommentSort.TOP, http = http)

        assertTrue(adapter.supportsChapterComments)
        assertEquals(10, page.totalCount)
        assertEquals("86", page.comments.single().id)
        assertEquals("chapter comment", page.comments.single().plainText)
        assertEquals(listOf("https://example.test/manga/sample/glava-86/?sort=best"), requests)
    }

    @Test
    fun `MangaShi support is domain based`() {
        val adapter = MangaShiCommentAdapter()
        assertTrue(adapter.supports(1L, "https://manga-shi.org"))
        assertTrue(adapter.supports(999L, "https://www.manga-shi.org"))
        assertFalse(adapter.supports(1L, "https://example.org"))
    }

    private companion object {
        const val PAGE_ONE = """
            <section id="comments">
              <div>49 комментариев</div>
              <div id="comments-list">
                <div id="comment-10" data-comment-fragment x-data="{ init() { const createdAt = new Date('2026-04-20T22:46:01.701400+03:00'); } }">
                  <div class="flex items-start gap-3">
                    <a href="/profile/alice/"><img src="/media/alice.jpg"></a>
                    <div class="flex-1 min-w-0">
                      <div class="flex flex-wrap items-center gap-2">
                        <a href="/profile/alice/">Alice</a>
                        <span class="px-2 py-0.5 text-[10px] uppercase">Ветеран</span>
                        <span class="text-[11px]">Спойлер</span>
                      </div>
                      <div class="mt-2 text-[13px] leading-relaxed">
                        <details><summary>Спойлер - показать</summary><div class="mt-2">secret <strong>text</strong></div></details>
                      </div>
                      <div id="comment-reactions-10">
                        <span><i class="ph ph-thumbs-up"></i><span>4</span></span>
                        <span><i class="ph ph-thumbs-down"></i><span>1</span></span>
                      </div>
                    </div>
                  </div>
                  <div class="c-replies">
                    <div id="replies-10">
                      <div>
                        <div id="comment-11" data-comment-fragment x-data="{ init() { const createdAt = new Date('2026-04-21T01:00:00+03:00'); } }">
                          <div class="flex items-start gap-3">
                            <a href="/profile/bob/"></a>
                            <div class="flex-1 min-w-0">
                              <div class="flex flex-wrap items-center gap-2"><a href="/profile/bob/">Bob</a><span class="c-reply-to">в ответ @Alice</span></div>
                              <div class="mt-2 text-[13px] leading-relaxed">reply body</div>
                              <div id="comment-reactions-11"><span><i class="ph ph-thumbs-up"></i><span>0</span></span><span><i class="ph ph-thumbs-down"></i><span>0</span></span></div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div id="comments-load-more"><button hx-get="/comments/more/?manga_id=2321&amp;comments_page=2&amp;sort=new">Загрузить ещё</button></div>
            </section>
        """

        const val PAGE_TWO = """
            <div id="comment-12" data-comment-fragment x-data="{ init() { const createdAt = new Date('2026-04-19T10:00:00+03:00'); } }">
              <div class="flex items-start gap-3">
                <a href="/profile/carol/"></a>
                <div class="flex-1 min-w-0">
                  <div class="flex flex-wrap items-center gap-2"><a href="/profile/carol/">Carol</a></div>
                  <div class="mt-2 text-[13px] leading-relaxed">page two</div>
                </div>
              </div>
            </div>
            <div id="comments-load-more"></div>
        """

        const val CHAPTER_PAGE = """
            <section id="comments">
              <div>10 комментариев</div>
              <div id="comments-list">
                <div id="comment-86" data-comment-fragment x-data="{ init() { const createdAt = new Date('2026-08-27T12:00:00+03:00'); } }">
                  <div class="flex items-start gap-3">
                    <a href="/profile/reader/"></a>
                    <div class="flex-1 min-w-0">
                      <div class="flex flex-wrap items-center gap-2"><a href="/profile/reader/">Reader</a></div>
                      <div class="mt-2 text-[13px] leading-relaxed">chapter comment</div>
                    </div>
                  </div>
                </div>
              </div>
            </section>
        """
    }
}
