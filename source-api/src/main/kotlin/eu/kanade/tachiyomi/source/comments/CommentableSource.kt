package eu.kanade.tachiyomi.source.comments

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga

/**
 * Optional source capability for read-only comments.
 *
 * Extensions that support comments may implement this without changing the base Source contract.
 * Host UI must hide comment affordances when a source does not implement this interface.
 */
interface CommentableSource {
    suspend fun getMangaComments(manga: SManga, page: Int = 1): SourceCommentThread

    suspend fun getChapterComments(chapter: SChapter, page: Int = 1): SourceCommentThread
}

data class SourceCommentThread(
    val comments: List<SourceComment>,
    val totalCount: Int? = null,
    val hasNextPage: Boolean = false,
)

data class SourceComment(
    val id: String,
    val author: String,
    val body: String,
    val createdAt: Long? = null,
    val score: Int? = null,
    val replyCount: Int? = null,
    val avatarUrl: String? = null,
)
