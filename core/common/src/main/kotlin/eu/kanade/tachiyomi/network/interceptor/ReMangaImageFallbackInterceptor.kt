package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Temporary ReManga image fix (2026-10).
 *
 * ReManga's primary image CDN (`img*.reimg.org`) started rejecting non-browser clients, so
 * the extension (v1.4.116) shows blank pages while the website still works. The website's
 * reader handles this by retrying failed images on the chapter's `fallback_link`
 * (`https://img-reserve.reimg2.org/`), which the extension does not know about.
 *
 * Extensions build their client from [eu.kanade.tachiyomi.network.NetworkHelper.client], so this
 * interceptor is inherited without modifying the extension APK. It only touches requests to
 * ReManga image hosts and does the same thing as the website: on an error or HTML block page
 * it repeats the request on the reserve host. After a primary failure the reserve host is used
 * first for a while to avoid paying a failed round-trip per page.
 */
class ReMangaImageFallbackInterceptor : Interceptor {

    @Volatile
    private var preferReserveUntil = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isReMangaImage(request.url)) return chain.proceed(request)

        val reserveRequest = request.toReserve()
        val primaryFirst = System.currentTimeMillis() >= preferReserveUntil || reserveRequest == null
        val attempts = if (primaryFirst) {
            listOfNotNull(request.withSiteReferer(), reserveRequest)
        } else {
            listOf(reserveRequest!!, request.withSiteReferer())
        }

        var lastError: IOException? = null
        for ((index, attempt) in attempts.withIndex()) {
            val isLast = index == attempts.lastIndex
            val response = try {
                chain.proceed(attempt)
            } catch (e: IOException) {
                lastError = e
                if (attempt.url.host != RESERVE_HOST) markPrimaryFailed()
                continue
            }
            if (isLast || response.isImage()) return response
            response.close()
            if (attempt.url.host != RESERVE_HOST) markPrimaryFailed()
        }
        throw lastError ?: IOException("ReManga image unavailable: ${request.url}")
    }

    private fun markPrimaryFailed() {
        preferReserveUntil = System.currentTimeMillis() + PREFER_RESERVE_MS
    }

    private fun Response.isImage(): Boolean {
        if (!isSuccessful) return false
        // The extension rewrites Content-Type from the file extension, so sniff the body:
        // Cloudflare / DDoS-Guard block pages are HTML.
        val head = runCatching { peekBody(16).string() }.getOrDefault("")
        return !head.trimStart().startsWith("<")
    }

    private fun Request.withSiteReferer(): Request =
        if (header("Referer").isNullOrBlank()) newBuilder().header("Referer", SITE_REFERER).build() else this

    private fun Request.toReserve(): Request? {
        if (url.host == RESERVE_HOST) return null
        val reserveUrl = url.newBuilder().scheme("https").host(RESERVE_HOST).build()
        return newBuilder()
            .url(reserveUrl)
            .header("Referer", SITE_REFERER)
            .build()
    }

    companion object {
        private const val RESERVE_HOST = "img-reserve.reimg2.org"
        private const val SITE_REFERER = "https://remanga.org/"
        private const val PREFER_RESERVE_MS = 10 * 60 * 1000L

        private val IMAGE_HOST_SUFFIXES = listOf("reimg.org", "reimg2.org")

        internal fun isReMangaImage(url: HttpUrl): Boolean =
            url.encodedPath.startsWith("/images/") &&
                IMAGE_HOST_SUFFIXES.any { url.host == it || url.host.endsWith(".$it") }
    }
}
