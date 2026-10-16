package eu.kanade.domain.manga.interactor

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.domain.source.interactor.UpdateMangaFromRemote
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.interactor.getLinkedMangaReferences
import tachiyomi.domain.manga.model.LinkedMangaReference
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.model.linkedMangaMemo
import tachiyomi.domain.manga.model.preferredLinkedVersion
import tachiyomi.domain.manga.model.toLinkedReference
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.repository.TrackRepository
import kotlin.time.Clock

/**
 * Explicit links between editions of one work on different sources.
 *
 * Only the preferred edition is a library entry; the other editions stay in the database with their chapters,
 * downloads and history, so removal, categories and library updates need no special handling elsewhere.
 */
@Inject
@SingleIn(AppScope::class)
class LinkedMangaService(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
    private val networkToLocalManga: NetworkToLocalManga,
    private val updateMangaFromRemote: UpdateMangaFromRemote,
    private val getCategories: GetCategories,
    private val sourceManager: SourceManager,
    private val trackerManager: TrackerManager,
    private val trackRepository: TrackRepository,
) {
    private val mutex = Mutex()

    suspend fun versions(mangaId: Long): List<Version> {
        val group = group(mangaRepository.getMangaById(mangaId))
        return group.refs.map { ref ->
            val local = group.members.firstOrNull { it.matches(ref) }
            val chapters = local?.let { chapterRepository.getChapterByMangaId(it.id) }.orEmpty()
            Version(ref, local?.id, chapters.size, chapters.count { it.read }, group.preferred.matches(ref))
        }
    }

    suspend fun link(mangaId: Long, targetId: Long) = mutex.withLock {
        require(mangaId != targetId)
        val current = group(mangaRepository.getMangaById(mangaId))
        val target = group(mangaRepository.getMangaById(targetId))
        val members = (current.members + target.members).distinctBy { it.id }
        val refs = (current.refs + target.refs).distinctBy { it.source to it.url }
        // Keep the card that is already in the library; otherwise the edition the user started from.
        val preferred = members.firstOrNull { it.favorite && it.id == mangaId }
            ?: members.firstOrNull { it.favorite }
            ?: members.first { it.id == mangaId }
        writeGroup(Group(refs, members, preferred))
    }

    suspend fun switch(mangaId: Long, reference: LinkedMangaReference): SwitchResult = mutex.withLock {
        val current = mangaRepository.getMangaById(mangaId)
        val group = group(current)
        require(group.refs.any { it.source == reference.source && it.url == reference.url })
        if (current.matches(reference)) {
            writeGroup(group.copy(preferred = current))
            return@withLock SwitchResult(current.id, 0, 0, 0)
        }

        val target = (
            group.members.firstOrNull { it.matches(reference) } ?: networkToLocalManga(
                Manga.create().copy(source = reference.source, url = reference.url, title = reference.title),
            )
            ).let { updateMangaFromRemote(it, fetchDetails = true, fetchChapters = true).getOrThrow().manga }

        val originalTarget = chapterRepository.getChapterByMangaId(target.id)
        var targetChapters = originalTarget
        var skipped = 0
        for (member in group.members.filter { it.id != target.id }) {
            val plan = linkedMangaProgress(chapterRepository.getChapterByMangaId(member.id), targetChapters)
            skipped += plan.skippedChapters
            val byId = plan.updates.associateBy { it.id }
            targetChapters = targetChapters.map { chapter ->
                val update = byId[chapter.id] ?: return@map chapter
                chapter.copy(read = update.read ?: chapter.read, bookmark = update.bookmark ?: chapter.bookmark)
            }
        }
        val updates = targetChapters.zip(originalTarget).mapNotNull { (chapter, previous) ->
            if (chapter.read == previous.read && chapter.bookmark == previous.bookmark) return@mapNotNull null
            ChapterUpdate(
                id = chapter.id,
                read = chapter.read.takeIf { it != previous.read },
                bookmark = chapter.bookmark.takeIf { it != previous.bookmark },
            )
        }
        if (updates.isNotEmpty()) chapterRepository.updateAll(updates)
        copyTracking(current, target)

        val members = (group.members.filter { it.id != target.id } + target)
        writeGroup(Group(group.refs, members, target), notesFrom = current)
        SwitchResult(target.id, updates.count { it.read == true }, updates.count { it.bookmark == true }, skipped)
    }

    suspend fun unlink(mangaId: Long, reference: LinkedMangaReference) = mutex.withLock {
        val current = mangaRepository.getMangaById(mangaId)
        require(!current.matches(reference)) { "Current edition cannot be unlinked from itself" }
        val group = group(current)
        val removed = group.members.firstOrNull { it.matches(reference) }
        val remainingRefs = group.refs.filterNot { it.source == reference.source && it.url == reference.url }
        val remaining = group.members.filter { it.id != removed?.id }
        val preferred = group.preferred.takeIf { it.id != removed?.id } ?: current
        // Unlinking never adds or removes library entries: the separated edition keeps its own state.
        val updates = remaining.map { member ->
            MangaUpdate(
                id = member.id,
                memo = member.linkedMangaMemo(
                    versions = remainingRefs.takeIf { it.size > 1 }.orEmpty(),
                    preferred = preferred.toLinkedReference(),
                ),
            )
        } + listOfNotNull(removed?.let { MangaUpdate(id = it.id, memo = it.linkedMangaMemo(emptyList(), null)) })
        check(mangaRepository.updateLinkedEditions(updates)) { "Failed to save linked editions" }
    }

    /** Loads the group once per operation: references plus the local rows that exist for them. */
    private suspend fun group(manga: Manga): Group {
        val refs = mangaRepository.getLinkedMangaReferences(manga)
        val members = refs.mapNotNull { ref ->
            if (manga.matches(ref)) manga else mangaRepository.getMangaByUrlAndSourceId(ref.url, ref.source)
        }
        val preferred = manga.preferredLinkedVersion
            ?.let { ref -> members.firstOrNull { it.matches(ref) } }
            ?: members.firstOrNull { it.favorite }
            ?: manga
        return Group(refs, members, preferred)
    }

    /**
     * Persists memo links on every local member and makes [Group.preferred] the only library entry when the
     * work is in the library. Categories of all library editions are merged onto the preferred one.
     */
    private suspend fun writeGroup(group: Group, notesFrom: Manga? = null) {
        val libraryMembers = group.members.filter { it.favorite }
        val inLibrary = libraryMembers.isNotEmpty()
        val preferredRef = group.preferred.toLinkedReference()
        val categories = libraryMembers.flatMap { getCategories.await(it.id) }.map { it.id }.distinct()
        val dateAdded = libraryMembers.minOfOrNull { it.dateAdded }?.takeIf { it > 0 }
            ?: Clock.System.now().toEpochMilliseconds()

        val updates = group.members.map { member ->
            val isPreferred = member.id == group.preferred.id
            val favorite = inLibrary && isPreferred
            MangaUpdate(
                id = member.id,
                memo = member.linkedMangaMemo(group.refs, preferredRef),
                favorite = favorite.takeIf { it != member.favorite },
                dateAdded = (if (favorite) dateAdded else 0L).takeIf { favorite != member.favorite },
                notes = notesFrom?.notes?.takeIf { isPreferred && member.notes.isBlank() && it.isNotBlank() },
            )
        }
        check(mangaRepository.updateLinkedEditions(updates)) { "Failed to save linked editions" }
        if (inLibrary) mangaRepository.setMangaCategories(group.preferred.id, categories)
    }

    private suspend fun copyTracking(current: Manga, target: Manga) {
        val existing = trackRepository.getTracksByMangaId(target.id).mapTo(hashSetOf()) { it.trackerId }
        val currentSource = sourceManager.get(current.source)
        val targetSource = sourceManager.get(target.source) ?: return
        val enhanced = trackerManager.trackers.filterIsInstance<EnhancedTracker>()
        val copied = trackRepository.getTracksByMangaId(current.id).mapNotNull { track ->
            if (track.trackerId in existing) return@mapNotNull null
            val destination = track.copy(mangaId = target.id)
            val tracker = enhanced.firstOrNull { it.isTrackFrom(destination, current, currentSource) }
            if (tracker == null) destination else tracker.migrateTrack(destination, target, targetSource)
        }
        if (copied.isNotEmpty()) trackRepository.insertAll(copied)
    }

    private fun Manga.matches(ref: LinkedMangaReference) = source == ref.source && url == ref.url

    private data class Group(val refs: List<LinkedMangaReference>, val members: List<Manga>, val preferred: Manga)

    data class Version(
        val reference: LinkedMangaReference,
        val mangaId: Long?,
        val chapters: Int,
        val read: Int,
        val preferred: Boolean,
    )

    data class SwitchResult(val mangaId: Long, val read: Int, val bookmarks: Int, val skipped: Int)
}
