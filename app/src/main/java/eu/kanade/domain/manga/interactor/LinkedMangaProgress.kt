package eu.kanade.domain.manga.interactor

import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate

data class LinkedMangaProgress(
    val updates: List<ChapterUpdate>,
    val skippedChapters: Int,
)

/** Exact, unambiguous chapter numbers only. Page positions differ between editions. */
fun linkedMangaProgress(source: List<Chapter>, target: List<Chapter>): LinkedMangaProgress {
    fun List<Chapter>.numbered() = filter { it.chapterNumber.isFinite() && it.chapterNumber >= 0 }
        .groupBy { it.chapterNumber }

    val sourceNumbers = source.numbered()
    val targetNumbers = target.numbered()
    val updates = mutableListOf<ChapterUpdate>()
    var skipped = 0
    for (chapter in source.filter { it.read || it.bookmark || it.lastPageRead > 0 }) {
        val destination = targetNumbers[chapter.chapterNumber]?.singleOrNull()
        if (sourceNumbers[chapter.chapterNumber]?.size != 1 || destination == null) {
            skipped++
            continue
        }
        val sourceVolume = VOLUME_NUMBER.find(chapter.name)?.groupValues?.get(1)?.toLongOrNull()
        val targetVolume = VOLUME_NUMBER.find(destination.name)?.groupValues?.get(1)?.toLongOrNull()
        if (sourceVolume != null && targetVolume != null && sourceVolume != targetVolume) {
            skipped++
            continue
        }
        if (!chapter.read && chapter.lastPageRead > 0) skipped++
        val copyRead = chapter.read && !destination.read
        val copyBookmark = chapter.bookmark && !destination.bookmark
        if (copyRead || copyBookmark) {
            updates += ChapterUpdate(
                id = destination.id,
                read = true.takeIf { copyRead },
                bookmark = true.takeIf { copyBookmark },
            )
        }
    }
    return LinkedMangaProgress(updates, skipped)
}

private val VOLUME_NUMBER = Regex("(?:том|volume|vol\\.?)\\s*(\\d+)", RegexOption.IGNORE_CASE)
