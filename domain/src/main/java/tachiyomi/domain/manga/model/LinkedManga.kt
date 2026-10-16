package tachiyomi.domain.manga.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

private const val LINKED_VERSIONS_KEY = "inkflow.linked.versions"
private const val PREFERRED_VERSION_KEY = "inkflow.linked.preferred"

data class LinkedMangaReference(
    val source: Long,
    val url: String,
    val title: String,
)

val Manga.linkedVersions: List<LinkedMangaReference>
    get() = memo.linkedVersions()

val Manga.preferredLinkedVersion: LinkedMangaReference?
    get() = memo.preferredLinkedVersion()

fun Manga.toLinkedReference(): LinkedMangaReference = LinkedMangaReference(source, url, title)

fun Manga.linkedMangaMemo(
    versions: List<LinkedMangaReference>,
    preferred: LinkedMangaReference?,
): JsonObject = memo.withLinkedVersions(versions, preferred)

/** Keep user-owned links even when source metadata is replaced or comes from an older request. */
fun preserveLinkedMangaMemo(localMemo: JsonObject, remoteMemo: JsonObject): JsonObject {
    val result = remoteMemo.toMutableMap()
    listOf(LINKED_VERSIONS_KEY, PREFERRED_VERSION_KEY).forEach { key ->
        result.remove(key)
        localMemo[key]?.let { result[key] = it }
    }
    return JsonObject(result)
}

/**
 * Restoring into an existing database keeps the local links when there are any, so an older backup cannot
 * bring back a link the user removed; a fresh install takes the backup's portable source/URL references.
 */
fun mergeRestoredLinkedMangaMemo(
    localMemo: JsonObject,
    backupMemo: JsonObject,
    restoredMemo: JsonObject,
): JsonObject {
    val origin = localMemo.takeIf { it.linkedVersions().isNotEmpty() } ?: backupMemo
    return restoredMemo.withLinkedVersions(origin.linkedVersions(), origin.preferredLinkedVersion())
}

private fun JsonObject.linkedVersions(): List<LinkedMangaReference> =
    (this[LINKED_VERSIONS_KEY] as? JsonArray)
        ?.mapNotNull { (it as? JsonObject)?.toLinkedReference() }
        ?.distinctBy { it.identity() }
        .orEmpty()

private fun JsonObject.preferredLinkedVersion(): LinkedMangaReference? {
    val preferred = (this[PREFERRED_VERSION_KEY] as? JsonObject)?.toLinkedReference() ?: return null
    return linkedVersions().firstOrNull { it.identity() == preferred.identity() }
}

private fun JsonObject.toLinkedReference(): LinkedMangaReference? {
    val source = (this["source"] as? JsonPrimitive)?.longOrNull ?: return null
    val url = (this["url"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?.takeIf { it.isNotBlank() } ?: return null
    val title = (this["title"] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()
    return LinkedMangaReference(source, url, title)
}

private fun JsonObject.withLinkedVersions(
    versions: List<LinkedMangaReference>,
    preferred: LinkedMangaReference?,
): JsonObject {
    val validVersions = versions
        .filter { it.url.isNotBlank() }
        .distinctBy { it.identity() }
    val validPreferred = preferred?.let { selected ->
        validVersions.firstOrNull { it.identity() == selected.identity() }
    }
    return JsonObject(
        toMutableMap().apply {
            remove(LINKED_VERSIONS_KEY)
            remove(PREFERRED_VERSION_KEY)
            if (validVersions.isNotEmpty()) {
                put(LINKED_VERSIONS_KEY, JsonArray(validVersions.map { it.toJsonObject() }))
                validPreferred?.let { put(PREFERRED_VERSION_KEY, it.toJsonObject()) }
            }
        },
    )
}

private fun LinkedMangaReference.identity(): Pair<Long, String> = source to url

private fun LinkedMangaReference.toJsonObject(): JsonObject = buildJsonObject {
    put("source", source)
    put("url", url)
    put("title", title)
}
