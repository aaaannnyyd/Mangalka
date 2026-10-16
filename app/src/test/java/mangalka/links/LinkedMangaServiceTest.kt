package mangalka.links

import eu.kanade.domain.manga.interactor.LinkedMangaService
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.domain.source.models.RemoteMangaUpdate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.LinkedMangaReference
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.model.linkedMangaMemo
import tachiyomi.domain.manga.model.linkedVersions
import tachiyomi.domain.manga.model.preferredLinkedVersion
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository
import java.io.IOException

class LinkedMangaServiceTest {

    private val first = LinkedMangaReference(1, "/first", "Same title")
    private val second = LinkedMangaReference(2, "/second", "Same title")

    @Test
    fun `switch preserves gaps pages original chapter state and existing target progress`() = runBlocking {
        val refs = listOf(first, second)
        val a = manga(11, first, refs).copy(notes = "Private note")
        val b = manga(12, second, refs)
        val sourceChapters = listOf(
            chapter(1, 11, 1.0, read = true, bookmark = true, page = 5),
            chapter(2, 11, 2.0),
            chapter(3, 11, 3.0, read = true),
            chapter(4, 11, 4.0, page = 90),
        )
        val targetChapters = listOf(
            chapter(21, 12, 1.0, page = 2),
            chapter(22, 12, 2.0, page = 10),
            chapter(23, 12, 3.0, read = true, bookmark = true, page = 20),
            chapter(24, 12, 4.0, page = 7),
        )
        val harness = Harness(listOf(a, b), mapOf(11L to sourceChapters, 12L to targetChapters))

        val result = harness.service.switch(11, second)

        assertEquals(12L, result.mangaId)
        assertEquals(1, result.read)
        assertEquals(1, result.bookmarks)
        assertEquals(1, result.skipped)
        val chapters = harness.chapters.getValue(12).associateBy { it.id }
        assertTrue(chapters.getValue(21).read)
        assertTrue(chapters.getValue(21).bookmark)
        assertFalse(chapters.getValue(22).read)
        assertFalse(chapters.getValue(24).read)
        assertEquals(targetChapters.map { it.lastPageRead }, harness.chapters.getValue(12).map { it.lastPageRead })
        assertEquals(sourceChapters, harness.chapters.getValue(11))
        assertEquals(
            a.copy(memo = harness.saved.getValue(11).memo, favorite = false, dateAdded = 0),
            harness.saved.getValue(11),
        )
        assertEquals("Private note", harness.saved.getValue(12).notes)
        assertTrue(harness.saved.getValue(12).favorite)
        assertTrue(harness.saved.values.all { it.preferredLinkedVersion == second })
        assertTrue(
            harness.chapterUpdates.flatten().all {
                it.lastPageRead == null && it.dateFetch == null && it.dateUpload == null && it.memo == null
            },
        )
    }

    @Test
    fun `failed source fetch leaves both editions metadata and progress unchanged`() {
        val refs = listOf(first, second)
        val a = manga(11, first, refs)
        val b = manga(12, second, refs)
        val sourceChapters = listOf(chapter(1, 11, 1.0, read = true))
        val targetChapters = listOf(chapter(21, 12, 1.0))
        val harness = Harness(listOf(a, b), mapOf(11L to sourceChapters, 12L to targetChapters))
        coEvery {
            harness.remote(any<Manga>(), fetchDetails = true, fetchChapters = true)
        } returns Result.failure(IOException("Offline"))

        assertThrows(IOException::class.java) { runBlocking { harness.service.switch(11, second) } }

        assertEquals(mapOf(11L to a, 12L to b), harness.saved)
        assertEquals(mapOf(11L to sourceChapters, 12L to targetChapters), harness.chapters)
        coVerify(exactly = 0) { harness.repository.updateLinkedEditions(any()) }
        coVerify(exactly = 0) { harness.chapterRepository.updateAll(any()) }
        coVerify(exactly = 0) { harness.tracks.insertAll(any()) }
    }

    @Test
    fun `database rejection does not report a successful preferred source change`() {
        val refs = listOf(first, second)
        val a = manga(11, first, refs)
        val b = manga(12, second, refs)
        val harness = Harness(listOf(a, b))
        coEvery { harness.repository.updateLinkedEditions(any()) } returns false

        assertThrows(IllegalStateException::class.java) { runBlocking { harness.service.switch(11, second) } }

        assertEquals(mapOf(11L to a, 12L to b), harness.saved)
    }

    @Test
    fun `unlink prevents removing current identity even if its displayed title changed`() {
        val refs = listOf(first, second)
        val harness = Harness(listOf(manga(11, first, refs), manga(12, second, refs)))

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { harness.service.unlink(11, first.copy(title = "Different label")) }
        }

        coVerify(exactly = 0) { harness.repository.updateLinkedEditions(any()) }
    }

    @Test
    fun `unlink accepts another source with same title and keeps both editions and chapters`() = runBlocking {
        val refs = listOf(first, second)
        val sourceChapters = listOf(chapter(1, 11, 1.0, read = true, page = 50))
        val targetChapters = listOf(chapter(21, 12, 1.0, bookmark = true))
        val harness = Harness(
            listOf(manga(11, first, refs), manga(12, second, refs)),
            mapOf(11L to sourceChapters, 12L to targetChapters),
        )

        harness.service.unlink(11, second)

        assertEquals(2, harness.saved.size)
        assertTrue(harness.saved.values.all { it.favorite && it.linkedVersions.isEmpty() })
        assertEquals(mapOf(11L to sourceChapters, 12L to targetChapters), harness.chapters)
        coVerify(exactly = 0) { harness.chapterRepository.removeChaptersWithIds(any()) }
    }

    @Test
    fun `explicit link keeps the library card and does not add the other edition to the library`() = runBlocking {
        val a = manga(11, first)
        val b = manga(12, second).copy(favorite = false)
        val harness = Harness(listOf(a, b))
        coEvery { harness.categories.await(11) } returns listOf(Category(7, "Reading", 1, 0))

        harness.service.link(11, 12)

        assertTrue(harness.saved.getValue(11).favorite)
        assertFalse(harness.saved.getValue(12).favorite)
        assertTrue(harness.saved.values.all { it.linkedVersions.toSet() == setOf(first, second) })
        assertTrue(harness.saved.values.all { it.preferredLinkedVersion == first })
        coVerify { harness.repository.setMangaCategories(11, listOf(7)) }
        coVerify(exactly = 0) { harness.repository.setMangaCategories(12, any()) }
        coVerify(exactly = 0) { harness.chapterRepository.updateAll(any()) }
    }

    @Test
    fun `linking two library entries leaves one card with merged categories`() = runBlocking {
        val a = manga(11, first).copy(dateAdded = 500)
        val b = manga(12, second).copy(dateAdded = 100)
        val harness = Harness(listOf(a, b))
        coEvery { harness.categories.await(11) } returns listOf(Category(7, "Reading", 1, 0))
        coEvery { harness.categories.await(12) } returns listOf(Category(8, "Favorite", 2, 0))

        harness.service.link(12, 11)

        assertTrue(harness.saved.getValue(12).favorite)
        assertFalse(harness.saved.getValue(11).favorite)
        assertEquals(100L, harness.saved.getValue(12).dateAdded)
        assertTrue(harness.saved.values.all { it.preferredLinkedVersion == second })
        coVerify { harness.repository.setMangaCategories(12, listOf(8, 7)) }
    }

    @Test
    fun `switch keeps existing target tracker and notes while copying missing tracker locally`() = runBlocking {
        val refs = listOf(first, second)
        val a = manga(11, first, refs).copy(notes = "Source notes")
        val b = manga(12, second, refs).copy(notes = "Target notes")
        val harness = Harness(listOf(a, b))
        val currentExisting = track(1, 11, 2, 100)
        val currentMissing = track(2, 11, 3, 200)
        val targetExisting = track(3, 12, 2, 900)
        coEvery { harness.tracks.getTracksByMangaId(11) } returns listOf(currentExisting, currentMissing)
        coEvery { harness.tracks.getTracksByMangaId(12) } returns listOf(targetExisting)

        harness.service.switch(11, second)

        assertEquals(listOf(currentMissing.copy(mangaId = 12)), harness.insertedTracks)
        assertEquals("Target notes", harness.saved.getValue(12).notes)
        coVerify(exactly = 0) { harness.tracks.delete(any(), any()) }
    }

    @Test
    fun `switch refuses a same titled edition that was not explicitly linked`() {
        val harness = Harness(listOf(manga(11, first), manga(12, second)))

        assertThrows(IllegalArgumentException::class.java) { runBlocking { harness.service.switch(11, second) } }

        assertNull(harness.saved.getValue(11).preferredLinkedVersion)
        coVerify(exactly = 0) { harness.remote(any<Manga>(), fetchDetails = true, fetchChapters = true) }
    }

    @Test
    fun `choosing currently open edition updates group preference without refetching or transferring progress`() =
        runBlocking {
            val refs = listOf(first, second)
            val a = manga(11, first, refs).let { it.copy(memo = it.linkedMangaMemo(refs, second)) }
            val b = manga(12, second, refs).let { it.copy(memo = it.linkedMangaMemo(refs, second)) }
            val harness = Harness(listOf(a, b))

            val result = harness.service.switch(11, first.copy(title = "Updated label"))

            assertEquals(11L, result.mangaId)
            assertEquals(0, result.read)
            assertEquals(0, result.bookmarks)
            assertTrue(harness.saved.values.all { it.preferredLinkedVersion == first })
            coVerify(exactly = 0) { harness.remote(any<Manga>(), fetchDetails = true, fetchChapters = true) }
            coVerify(exactly = 0) { harness.chapterRepository.updateAll(any()) }
        }

    private fun manga(id: Long, reference: LinkedMangaReference, refs: List<LinkedMangaReference> = emptyList()) =
        Manga.create().copy(
            id = id,
            source = reference.source,
            url = reference.url,
            title = reference.title,
            favorite = true,
        ).let { it.copy(memo = it.linkedMangaMemo(refs, first)) }

    private fun chapter(
        id: Long,
        mangaId: Long,
        number: Double,
        read: Boolean = false,
        bookmark: Boolean = false,
        page: Long = 0,
    ) = Chapter.create().copy(
        id = id,
        mangaId = mangaId,
        url = "/chapter/$id",
        chapterNumber = number,
        read = read,
        bookmark = bookmark,
        lastPageRead = page,
    )

    private fun track(id: Long, mangaId: Long, trackerId: Long, remoteId: Long) = Track(
        id = id,
        mangaId = mangaId,
        trackerId = trackerId,
        remoteId = remoteId,
        libraryId = null,
        title = "Tracked work",
        lastChapterRead = 5.0,
        totalChapters = 10,
        status = 1,
        score = 8.0,
        remoteUrl = "https://tracker.test/$remoteId",
        startDate = 0,
        finishDate = 0,
        private = false,
    )

    private class Harness(mangas: List<Manga>, chapterLists: Map<Long, List<Chapter>> = emptyMap()) {
        val saved = mangas.associateBy { it.id }.toMutableMap()
        val chapters = chapterLists.toMutableMap()
        val chapterUpdates = mutableListOf<List<ChapterUpdate>>()
        val insertedTracks = mutableListOf<Track>()
        val repository = mockk<MangaRepository>()
        val chapterRepository = mockk<ChapterRepository>()
        val remote = mockk<UpdateMangaFromRemote>()
        val categories = mockk<GetCategories>()
        val sources = mockk<SourceManager>()
        val trackers = mockk<TrackerManager>()
        val tracks = mockk<TrackRepository>()
        val service: LinkedMangaService

        init {
            coEvery { repository.getFavorites() } answers { saved.values.filter { it.favorite } }
            coEvery { repository.getMangaById(any()) } answers { saved.getValue(firstArg<Long>()) }
            coEvery { repository.getMangaByUrlAndSourceId(any(), any()) } answers {
                saved.values.firstOrNull { it.url == firstArg<String>() && it.source == secondArg<Long>() }
            }
            coEvery { repository.updateLinkedEditions(any()) } answers {
                firstArg<List<MangaUpdate>>().forEach { update ->
                    val current = saved.getValue(update.id)
                    saved[update.id] = current.copy(
                        favorite = update.favorite ?: current.favorite,
                        dateAdded = update.dateAdded ?: current.dateAdded,
                        memo = update.memo ?: current.memo,
                        notes = update.notes ?: current.notes,
                    )
                }
                true
            }
            coEvery { repository.setMangaCategories(any(), any()) } returns Unit
            coEvery { repository.updateAll(any()) } returns true
            coEvery { categories.await(any()) } returns emptyList()
            coEvery { chapterRepository.getChapterByMangaId(any(), any()) } answers {
                chapters[firstArg<Long>()].orEmpty()
            }
            coEvery { chapterRepository.updateAll(any()) } answers {
                val updates = firstArg<List<ChapterUpdate>>()
                chapterUpdates += updates
                val byId = updates.associateBy { it.id }
                chapters.keys.toList().forEach { mangaId ->
                    chapters[mangaId] = chapters.getValue(mangaId).map { chapter ->
                        val update = byId[chapter.id]
                        chapter.copy(
                            read = update?.read ?: chapter.read,
                            bookmark = update?.bookmark ?: chapter.bookmark,
                            lastPageRead = update?.lastPageRead ?: chapter.lastPageRead,
                        )
                    }
                }
                Unit
            }
            coEvery { remote(any<Manga>(), fetchDetails = true, fetchChapters = true) } answers {
                Result.success(RemoteMangaUpdate(saved.getValue(firstArg<Manga>().id), emptyList()))
            }
            every { sources.get(any()) } returns mockk<Source>()
            every { trackers.trackers } returns emptyList()
            coEvery { tracks.getTracksByMangaId(any()) } returns emptyList()
            coEvery { tracks.insertAll(any()) } answers {
                insertedTracks += firstArg<List<Track>>()
                Unit
            }
            service = LinkedMangaService(
                repository,
                chapterRepository,
                mockk<NetworkToLocalManga>(),
                remote,
                categories,
                sources,
                trackers,
                tracks,
            )
        }
    }
}
