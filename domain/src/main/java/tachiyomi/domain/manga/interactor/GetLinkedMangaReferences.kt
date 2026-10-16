package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.LinkedMangaReference
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.linkedVersions
import tachiyomi.domain.manga.model.toLinkedReference
import tachiyomi.domain.manga.repository.MangaRepository

/** Expand overlapping restored groups without relying on database IDs or matching titles. */
suspend fun MangaRepository.getLinkedMangaReferences(manga: Manga): List<LinkedMangaReference> {
    val favorites = getFavorites()
    val localByIdentity = favorites.associateBy { it.source to it.url }
    val referencing = favorites.flatMap { member ->
        (member.linkedVersions + member.toLinkedReference()).map { (it.source to it.url) to member }
    }.groupBy({ it.first }, { it.second })
    val pending = ArrayDeque(manga.linkedVersions + manga.toLinkedReference())
    val visited = linkedMapOf<Pair<Long, String>, LinkedMangaReference>()
    while (pending.isNotEmpty()) {
        val ref = pending.removeFirst()
        val identity = ref.source to ref.url
        if (identity in visited) continue
        val local = localByIdentity[identity] ?: getMangaByUrlAndSourceId(ref.url, ref.source)
        visited[identity] = local?.toLinkedReference() ?: ref
        local?.linkedVersions?.forEach { linked ->
            if ((linked.source to linked.url) !in visited) pending.addLast(linked)
        }
        referencing[identity].orEmpty().forEach { member ->
            if ((member.source to member.url) !in visited) pending.addLast(member.toLinkedReference())
        }
    }
    return visited.values.toList()
}
