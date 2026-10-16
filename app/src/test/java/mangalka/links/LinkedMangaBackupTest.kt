package mangalka.links

import eu.kanade.tachiyomi.data.backup.models.BackupManga
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.domain.manga.model.LinkedMangaReference
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.linkedMangaMemo
import tachiyomi.domain.manga.model.linkedVersions
import tachiyomi.domain.manga.model.preferredLinkedVersion

@OptIn(ExperimentalSerializationApi::class)
class LinkedMangaBackupTest {

    @Test
    fun `existing protobuf memo field preserves source links without database identifiers`() {
        val original = LinkedMangaReference(1, "/original", "Original")
        val alternate = LinkedMangaReference(2, "/alternate", "Alternate")
        val manga = Manga.create().copy(id = 19, source = original.source, url = original.url, title = original.title)
        val backup = BackupManga(
            source = manga.source,
            url = manga.url,
            title = manga.title,
            memo = MemoColumnAdapter.encode(manga.linkedMangaMemo(listOf(original, alternate), alternate)),
        )

        val encoded = ProtoBuf.encodeToByteArray(BackupManga.serializer(), backup)
        val restored = ProtoBuf.decodeFromByteArray(BackupManga.serializer(), encoded).getMangaImpl().copy(id = 500)

        assertEquals(listOf(original, alternate), restored.linkedVersions)
        assertEquals(alternate, restored.preferredLinkedVersion)
        assertEquals(500L, restored.id)
    }
}
