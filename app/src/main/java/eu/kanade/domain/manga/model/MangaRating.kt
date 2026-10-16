package eu.kanade.domain.manga.model

import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import tachiyomi.domain.manga.model.Manga
import java.util.Locale

data class MangaRating(
    val value: Double,
    val scale: Double,
    val count: Long?,
) {
    val displayValue: String
        get() {
            val valueText = if (value % 1.0 == 0.0) {
                value.toInt().toString()
            } else {
                String.format(Locale.US, "%.1f", value)
            }
            val scaleText = if (scale % 1.0 == 0.0) {
                scale.toInt().toString()
            } else {
                String.format(Locale.US, "%.1f", scale)
            }
            return "$valueText/$scaleText"
        }
}

/**
 * App-owned rating metadata contract. Unknown ratings stay absent instead of becoming a fake 0.
 *
 * Preferred memo keys:
 * - inkflow.rating.value
 * - inkflow.rating.scale
 * - inkflow.rating.count
 *
 * Nested `inkflow.rating.{value,scale,count}` is accepted too for forward compatibility.
 */
fun Manga.sourceRating(): MangaRating? {
    val nestedRating = runCatching {
        memo["inkflow"]?.jsonObject
            ?.get("rating")?.jsonObject
    }.getOrNull()

    val value = memo["inkflow.rating.value"]?.jsonPrimitive?.doubleOrNull
        ?: nestedRating?.get("value")?.jsonPrimitive?.doubleOrNull
        ?: return null
    val scale = memo["inkflow.rating.scale"]?.jsonPrimitive?.doubleOrNull
        ?: nestedRating?.get("scale")?.jsonPrimitive?.doubleOrNull
        ?: 10.0
    val count = memo["inkflow.rating.count"]?.jsonPrimitive?.longOrNull
        ?: nestedRating?.get("count")?.jsonPrimitive?.longOrNull

    if (!value.isFinite() || !scale.isFinite() || scale <= 0.0 || value < 0.0) return null
    return MangaRating(value = value, scale = scale, count = count?.takeIf { it >= 0L })
}
