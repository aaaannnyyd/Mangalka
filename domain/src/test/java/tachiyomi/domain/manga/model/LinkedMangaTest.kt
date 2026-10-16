package tachiyomi.domain.manga.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LinkedMangaTest {

    private val original = LinkedMangaReference(1, "/original", "Original")
    private val alternate = LinkedMangaReference(2, "/alternate", "Alternate")
    private val third = LinkedMangaReference(3, "/third", "Third")

    @Test
    fun `links retain stable source and URL identity after database IDs change`() {
        val manga = Manga.create().copy(id = 7, source = original.source, url = original.url, title = original.title)
        val memo = manga.linkedMangaMemo(listOf(original, alternate), alternate)
        val restored = manga.copy(id = 900, memo = Json.parseToJsonElement(memo.toString()) as JsonObject)

        assertEquals(listOf(original, alternate), restored.linkedVersions)
        assertEquals(alternate, restored.preferredLinkedVersion)
        assertEquals(original, restored.toLinkedReference())
    }

    @Test
    fun `deduplication uses source and URL rather than title`() {
        val sameTitle = third.copy(title = original.title)
        val sameUrl = third.copy(url = original.url)
        val changedTitle = original.copy(title = "Changed")
        val manga = Manga.create().let {
            it.copy(memo = it.linkedMangaMemo(listOf(original, changedTitle, sameTitle, sameUrl), changedTitle))
        }

        assertEquals(listOf(original, sameTitle, sameUrl), manga.linkedVersions)
        assertEquals(original, manga.preferredLinkedVersion)
    }

    @Test
    fun `malformed metadata skips invalid entries and ignores unrelated preferred version`() {
        val memo = Json.parseToJsonElement(
            """
            {
              "inkflow.linked.versions": [
                null,
                "invalid",
                {"source": "not-an-id", "url": "/invalid", "title": "Invalid"},
                {"source": 2, "url": "   ", "title": "Invalid"},
                {"source": 2, "url": 5, "title": "Invalid"},
                {"source": [], "url": "/invalid"},
                {"source": 1, "url": "/original", "title": "Original"}
              ],
              "inkflow.linked.preferred": {"source": 9, "url": "/missing", "title": "Missing"}
            }
            """.trimIndent(),
        ) as JsonObject
        val manga = Manga.create().copy(memo = memo)

        assertEquals(listOf(original), manga.linkedVersions)
        assertNull(manga.preferredLinkedVersion)
        assertEquals(emptyList<LinkedMangaReference>(), Manga.create().linkedVersions)
    }

    @Test
    fun `unlink clears both reserved keys and keeps other metadata`() {
        val baseMemo = JsonObject(mapOf("source.rating" to JsonPrimitive(9)))
        val manga = Manga.create().copy(memo = baseMemo).let {
            it.copy(memo = it.linkedMangaMemo(listOf(original, alternate), alternate))
        }

        val unlinked = manga.linkedMangaMemo(emptyList(), alternate)

        assertEquals(baseMemo, unlinked)
    }

    @Test
    fun `source refresh replaces source metadata while retaining current user links`() {
        val local = Manga.create().copy(memo = JsonObject(mapOf("source.rating" to JsonPrimitive(3))))
        val localMemo = local.linkedMangaMemo(listOf(original, alternate), alternate)
        val stale = Manga.create().copy(memo = JsonObject(mapOf("source.rating" to JsonPrimitive(9))))
        val remoteMemo = stale.linkedMangaMemo(listOf(original, third), third)
        val result = local.copy(memo = preserveLinkedMangaMemo(localMemo, remoteMemo))

        assertEquals(listOf(original, alternate), result.linkedVersions)
        assertEquals(alternate, result.preferredLinkedVersion)
        assertEquals(JsonPrimitive(9), result.memo["source.rating"])
    }

    @Test
    fun `stale response cannot resurrect links removed while it was in flight`() {
        val remoteMemo = Manga.create().linkedMangaMemo(listOf(original, alternate), alternate)
        val localMemo = JsonObject(mapOf("local.other" to JsonPrimitive(true)))
        val result = preserveLinkedMangaMemo(localMemo, remoteMemo)

        assertFalse(result.containsKey("inkflow.linked.versions"))
        assertFalse(result.containsKey("inkflow.linked.preferred"))
        assertFalse(result.containsKey("local.other"))
    }

    @Test
    fun `backup restore keeps local links so an older backup cannot resurrect removed ones`() {
        val localMemo = Manga.create().linkedMangaMemo(listOf(original, alternate), alternate)
        val backupMemo = Manga.create().linkedMangaMemo(listOf(original, third), third)
        val restoredMemo = JsonObject(mapOf("source.rating" to JsonPrimitive(8)))
        val result = Manga.create().copy(
            memo = mergeRestoredLinkedMangaMemo(localMemo, backupMemo, restoredMemo),
        )

        assertEquals(listOf(original, alternate), result.linkedVersions)
        assertEquals(alternate, result.preferredLinkedVersion)
        assertEquals(JsonPrimitive(8), result.memo["source.rating"])
    }

    @Test
    fun `backup links are restored when the local edition has none`() {
        val localMemo = JsonObject(emptyMap())
        val backupMemo = Manga.create().linkedMangaMemo(listOf(original, third), third)
        val result = Manga.create().copy(
            memo = mergeRestoredLinkedMangaMemo(localMemo, backupMemo, localMemo),
        )

        assertEquals(listOf(original, third), result.linkedVersions)
        assertEquals(third, result.preferredLinkedVersion)
    }

    @Test
    fun `custom extension IDs retain the full Long range`() {
        val customSource = original.copy(source = Long.MIN_VALUE)
        val largeSource = alternate.copy(source = Long.MAX_VALUE)
        val manga = Manga.create().let {
            it.copy(memo = it.linkedMangaMemo(listOf(customSource, largeSource), customSource))
        }

        assertEquals(listOf(customSource, largeSource), manga.linkedVersions)
        assertEquals(customSource, manga.preferredLinkedVersion)
    }
}
