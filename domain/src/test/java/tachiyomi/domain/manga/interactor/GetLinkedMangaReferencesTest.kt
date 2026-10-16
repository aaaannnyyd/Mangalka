package tachiyomi.domain.manga.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.LinkedMangaReference
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.linkedMangaMemo
import tachiyomi.domain.manga.model.toLinkedReference
import tachiyomi.domain.manga.repository.MangaRepository

class GetLinkedMangaReferencesTest {

    private val first = LinkedMangaReference(1, "/first", "Same title")
    private val second = LinkedMangaReference(2, "/second", "Same title")
    private val third = LinkedMangaReference(3, "/third", "Same title")

    @Test
    fun `cyclic overlapping links expand each identity once including nonfavorite editions`() = runBlocking {
        val a = manga(11, first, listOf(first, second), favorite = false)
        val b = manga(12, second, listOf(second, third), favorite = false)
        val c = manga(13, third, listOf(third, first), favorite = false)
        val repository = repository(listOf(a, b, c))

        val refs = repository.getLinkedMangaReferences(a)

        assertEquals(setOf(first, second, third), refs.toSet())
        assertEquals(3, refs.size)
        coVerify(exactly = 1) { repository.getMangaByUrlAndSourceId(first.url, first.source) }
        coVerify(exactly = 1) { repository.getMangaByUrlAndSourceId(second.url, second.source) }
        coVerify(exactly = 1) { repository.getMangaByUrlAndSourceId(third.url, third.source) }
    }

    @Test
    fun `reverse favorite references recover one sided restored links`() = runBlocking {
        val a = manga(11, first)
        val b = manga(12, second, listOf(first, second))

        assertEquals(setOf(first, second), repository(listOf(a, b)).getLinkedMangaReferences(a).toSet())
    }

    @Test
    fun `missing reference bridges favorite groups without requiring the missing row`() = runBlocking {
        val a = manga(11, first, listOf(first, second))
        val c = manga(13, third, listOf(second, third))
        val repository = repository(listOf(a, c))

        assertEquals(setOf(first, second, third), repository.getLinkedMangaReferences(a).toSet())
        coVerify(exactly = 1) { repository.getMangaByUrlAndSourceId(second.url, second.source) }
    }

    @Test
    fun `equal titles never create a connection`() = runBlocking {
        val a = manga(11, first)
        val b = manga(12, second)

        assertEquals(listOf(first), repository(listOf(a, b)).getLinkedMangaReferences(a))
    }

    @Test
    fun `fresh local title replaces stale stored label while stable identity remains unchanged`() = runBlocking {
        val a = manga(11, first, listOf(first, second))
        val b = manga(900, second.copy(title = "Updated source title"))
        val refs = repository(listOf(a, b)).getLinkedMangaReferences(a)

        assertEquals(setOf(first, b.toLinkedReference()), refs.toSet())
    }

    private fun manga(
        id: Long,
        reference: LinkedMangaReference,
        refs: List<LinkedMangaReference> = emptyList(),
        favorite: Boolean = true,
    ) = Manga.create().copy(
        id = id,
        source = reference.source,
        url = reference.url,
        title = reference.title,
        favorite = favorite,
    ).let { it.copy(memo = it.linkedMangaMemo(refs, null)) }

    private fun repository(mangas: List<Manga>): MangaRepository {
        val repository = mockk<MangaRepository>()
        coEvery { repository.getFavorites() } returns mangas.filter { it.favorite }
        coEvery { repository.getMangaByUrlAndSourceId(any(), any()) } answers {
            mangas.firstOrNull { it.url == firstArg<String>() && it.source == secondArg<Long>() }
        }
        return repository
    }
}
