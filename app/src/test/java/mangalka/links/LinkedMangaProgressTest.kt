package mangalka.links

import eu.kanade.domain.manga.interactor.linkedMangaProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class LinkedMangaProgressTest {

    @Test
    fun `switch preserves gaps instead of marking every earlier chapter read`() {
        val source = listOf(chapter(1, 1.0, read = true), chapter(2, 2.0), chapter(3, 3.0, read = true))
        val target = listOf(chapter(11, 1.0), chapter(12, 2.0), chapter(13, 3.0))

        val plan = linkedMangaProgress(source, target)

        assertEquals(setOf(11L, 13L), plan.updates.map { it.id }.toSet())
        assertTrue(plan.updates.all { it.read == true })
        assertEquals(0, plan.skippedChapters)
    }

    @Test
    fun `multiple source branches make a chapter number ambiguous even if only one is read`() {
        val source = listOf(
            chapter(1, 1.0, read = true).copy(scanlator = "Team A"),
            chapter(2, 1.0).copy(scanlator = "Team B"),
        )

        val plan = linkedMangaProgress(source, listOf(chapter(11, 1.0)))

        assertTrue(plan.updates.isEmpty())
        assertEquals(1, plan.skippedChapters)
    }

    @Test
    fun `multiple target branches are skipped while unrelated unique chapters transfer`() {
        val source = listOf(chapter(1, 1.0, read = true), chapter(2, 2.0, bookmark = true))
        val target = listOf(
            chapter(11, 1.0).copy(scanlator = "Team A"),
            chapter(12, 1.0).copy(scanlator = "Team B"),
            chapter(13, 2.0),
        )

        val plan = linkedMangaProgress(source, target)

        assertEquals(1, plan.updates.size)
        assertEquals(13L, plan.updates.single().id)
        assertEquals(true, plan.updates.single().bookmark)
        assertEquals(1, plan.skippedChapters)
    }

    @Test
    fun `decimal chapters require exact finite numbers and chapter zero remains valid`() {
        val source = listOf(
            chapter(1, 0.0, read = true),
            chapter(2, 2.5, read = true),
            chapter(3, 3.5, read = true),
            chapter(4, -1.0, read = true),
            chapter(5, Double.NaN, read = true),
            chapter(6, Double.POSITIVE_INFINITY, read = true),
            chapter(7, Double.NEGATIVE_INFINITY, read = true),
        )
        val target = listOf(
            chapter(11, 0.0),
            chapter(12, 2.5),
            chapter(13, 3.5000000001),
            chapter(14, -1.0),
            chapter(15, Double.NaN),
            chapter(16, Double.POSITIVE_INFINITY),
            chapter(17, Double.NEGATIVE_INFINITY),
        )

        val plan = linkedMangaProgress(source, target)

        assertEquals(setOf(11L, 12L), plan.updates.map { it.id }.toSet())
        assertEquals(5, plan.skippedChapters)
    }

    @Test
    fun `transferred progress only changes read and bookmark flags`() {
        val source = chapter(1, 1.0, read = true, bookmark = true, page = 90)
            .copy(dateFetch = 100, dateUpload = 200, sourceOrder = 300, version = 9)
        val target = chapter(11, 1.0, page = 12)

        val update = linkedMangaProgress(listOf(source), listOf(target)).updates.single()

        assertEquals(11L, update.id)
        assertEquals(true, update.read)
        assertEquals(true, update.bookmark)
        assertNull(update.lastPageRead)
        assertNull(update.mangaId)
        assertNull(update.dateFetch)
        assertNull(update.dateUpload)
        assertNull(update.sourceOrder)
        assertNull(update.version)
        assertNull(update.memo)
        assertNull(update.url)
        assertNull(update.name)
        assertNull(update.scanlator)
        assertNull(update.chapterNumber)
        assertEquals(12L, target.lastPageRead)
    }

    @Test
    fun `existing target completion bookmark and page are never regressed`() {
        val source = listOf(
            chapter(1, 1.0, page = 4),
            chapter(2, 2.0, read = true),
            chapter(3, 3.0, bookmark = true),
        )
        val target = listOf(
            chapter(11, 1.0, read = true, bookmark = true, page = 12),
            chapter(12, 2.0, bookmark = true, page = 7),
            chapter(13, 3.0, read = true, page = 9),
        )

        val plan = linkedMangaProgress(source, target)
        val updates = plan.updates.associateBy { it.id }

        assertEquals(setOf(12L, 13L), updates.keys)
        assertEquals(true, updates.getValue(12).read)
        assertNull(updates.getValue(12).bookmark)
        assertEquals(true, updates.getValue(13).bookmark)
        assertNull(updates.getValue(13).read)
        assertTrue(plan.updates.all { it.lastPageRead == null && it.read != false && it.bookmark != false })
        assertEquals(1, plan.skippedChapters)
    }

    @Test
    fun `partial pages remain in old edition while an exact bookmark can transfer`() {
        val source = chapter(1, 1.0, bookmark = true, page = 20)

        val plan = linkedMangaProgress(listOf(source), listOf(chapter(11, 1.0)))

        assertEquals(1, plan.skippedChapters)
        assertEquals(true, plan.updates.single().bookmark)
        assertNull(plan.updates.single().read)
        assertNull(plan.updates.single().lastPageRead)
        assertEquals(20L, source.lastPageRead)
    }

    @Test
    fun `already transferred progress is idempotent`() {
        val source = chapter(1, 1.0, read = true, bookmark = true)
        val target = chapter(11, 1.0, read = true, bookmark = true, page = 45)

        val plan = linkedMangaProgress(listOf(source), listOf(target))

        assertTrue(plan.updates.isEmpty())
        assertEquals(0, plan.skippedChapters)
    }

    @Test
    fun `empty lists do not fabricate progress`() {
        assertTrue(linkedMangaProgress(emptyList(), listOf(chapter(11, 1.0))).updates.isEmpty())
        val noDestination = linkedMangaProgress(listOf(chapter(1, 1.0, read = true)), emptyList())
        assertTrue(noDestination.updates.isEmpty())
        assertEquals(1, noDestination.skippedChapters)
    }

    @Test
    fun `explicit volume mismatches skip progress while matching or absent volumes remain compatible`() {
        val source = listOf(
            chapter(1, 1.0, read = true).copy(name = "Том 2 Глава 1"),
            chapter(2, 2.0, read = true).copy(name = "Vol. 3 Ch. 2"),
            chapter(3, 3.0, read = true).copy(name = "Volume 4 Chapter 3"),
            chapter(4, 4.0, read = true).copy(name = "Том 2 Глава 4"),
        )
        val target = listOf(
            chapter(11, 1.0).copy(name = "Том 1 Глава 1"),
            chapter(12, 2.0).copy(name = "Volume 1 Chapter 2"),
            chapter(13, 3.0).copy(name = "vOL.4 Ch.3"),
            chapter(14, 4.0).copy(name = "Chapter 4"),
        )

        val plan = linkedMangaProgress(source, target)

        assertEquals(setOf(13L, 14L), plan.updates.map { it.id }.toSet())
        assertEquals(2, plan.skippedChapters)
    }

    private fun chapter(
        id: Long,
        number: Double,
        read: Boolean = false,
        bookmark: Boolean = false,
        page: Long = 0,
    ) = Chapter.create().copy(
        id = id,
        mangaId = id,
        url = "/chapter/$id",
        name = "Chapter $number",
        chapterNumber = number,
        read = read,
        bookmark = bookmark,
        lastPageRead = page,
    )
}
