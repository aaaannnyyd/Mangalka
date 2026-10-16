package mangalka.comments.adapter

import mangalka.comments.CommentAdapter
import mangalka.comments.CommentHttp
import mangalka.comments.model.CommentPage
import mangalka.comments.model.CommentSort
import mangalka.comments.model.CommentTarget
import java.net.URI

/**
 * HenChan uses the MultiChan/DLE stack and renders comments into the page HTML.
 *
 * A dedicated wrapper is intentionally tiny: parsing stays in [AutoCommentAdapter],
 * while this adapter gives HenChan an exact source/domain match and enables
 * chapter comments. Generic fallback keeps chapter comments disabled so we do
 * not expose an empty chapter-comments block for every unknown extension.
 */
class HenChanCommentAdapter(
    private val delegate: AutoCommentAdapter = AutoCommentAdapter(),
) : CommentAdapter {

    override val id = "henchan"
    override val name = "HenChan"
    override val supportsChapterComments = true

    override fun supports(sourceId: Long, baseUrl: String?): Boolean {
        if (sourceId == SOURCE_ID) return true
        val host = runCatching { URI(baseUrl).host.orEmpty().lowercase() }.getOrDefault("")
        return DOMAINS.any { host == it || host.endsWith(".$it") }
    }

    override suspend fun fetch(
        target: CommentTarget,
        page: Int,
        sort: CommentSort,
        http: CommentHttp,
    ): CommentPage {
        val resolvedTarget = when (target) {
            is CommentTarget.Chapter -> {
                // HenChan's extension stores legacy/exhentai chapters as `/manga/...`,
                // but pageListRequest reads them from `/online/...`. Comments live on
                // that same reader page, so mirror the extension's URL conversion.
                if (target.chapterUrl.contains("/manga/") && target.absoluteUrl?.contains("/manga/") == true) {
                    target.copy(absoluteUrl = target.absoluteUrl.replace("/manga/", "/online/"))
                } else {
                    target
                }
            }
            is CommentTarget.Manga -> target
        }
        return delegate.fetch(resolvedTarget, page, sort, http).copy(providerId = id)
    }

    private companion object {
        const val SOURCE_ID = 5504588601186153612L
        val DOMAINS = listOf(
            "hentaichan.live",
            "h-chan.me",
            "henchan.pro",
        )
    }
}
