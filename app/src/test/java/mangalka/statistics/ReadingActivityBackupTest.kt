package mangalka.statistics

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupReadingActivity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.readingactivity.model.ReadingActivityBackup

@OptIn(ExperimentalSerializationApi::class)
class ReadingActivityBackupTest {
    @Serializable
    private data class LegacyBackup(
        @ProtoNumber(1) val backupManga: List<BackupManga>,
    )

    @Test
    fun `old backup without journal still decodes`() {
        val bytes = ProtoBuf.encodeToByteArray(
            LegacyBackup.serializer(),
            LegacyBackup(listOf(BackupManga(source = 7, url = "/title", title = "Title"))),
        )
        val decoded = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
        assertTrue(decoded.backupReadingActivity.isEmpty())
        assertEquals("/title", decoded.backupManga.single().url)
    }

    @Test
    fun `journal roundtrip preserves portable identity and older reader skips optional field`() {
        val entry = ReadingActivityBackup(7, "/title", "/chapter", 20_731, 123_456, true)
        val backup = Backup(
            backupManga = listOf(BackupManga(source = 7, url = "/title", title = "Title")),
            backupReadingActivity = listOf(BackupReadingActivity.fromDomain(entry)),
        )
        val bytes = ProtoBuf.encodeToByteArray(Backup.serializer(), backup)
        val decoded = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
        assertEquals(entry, decoded.backupReadingActivity.single().toDomain())
        val older = ProtoBuf.decodeFromByteArray(LegacyBackup.serializer(), bytes)
        assertEquals("/title", older.backupManga.single().url)
    }
}
