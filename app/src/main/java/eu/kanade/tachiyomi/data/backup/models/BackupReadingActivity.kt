package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.readingactivity.model.ReadingActivityBackup

@Serializable
data class BackupReadingActivity(
    @ProtoNumber(1) val sourceId: Long,
    @ProtoNumber(2) val mangaUrl: String,
    @ProtoNumber(3) val chapterUrl: String,
    @ProtoNumber(4) val localDay: Long,
    @ProtoNumber(5) val durationMillis: Long = 0,
    @ProtoNumber(6) val completed: Boolean = false,
) {
    fun toDomain() = ReadingActivityBackup(sourceId, mangaUrl, chapterUrl, localDay, durationMillis, completed)

    companion object {
        fun fromDomain(entry: ReadingActivityBackup) = BackupReadingActivity(
            entry.sourceId,
            entry.mangaUrl,
            entry.chapterUrl,
            entry.day,
            entry.durationMillis,
            entry.completed,
        )
    }
}
