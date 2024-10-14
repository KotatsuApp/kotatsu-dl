package org.koitharu.kotatsu.dl.util

import androidx.collection.IntList
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.jsoup.HttpStatusException
import java.net.HttpURLConnection

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> List<T>.component6(): T = get(5)

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> List<T>.component7(): T = get(6)

inline fun String?.ifNullOrEmpty(fallback: () -> String): String = if (isNullOrEmpty()) fallback() else this

fun getFileExtensionFromUrl(url: String): String? {
    return url.toHttpUrlOrNull()?.pathSegments?.lastOrNull()?.substringAfterLast('.')?.takeIf { ext ->
        ext.length in 2..4
    }
}

fun Response.ensureSuccess() = apply {
    if (!isSuccessful || code == HttpURLConnection.HTTP_NO_CONTENT) {
        closeQuietly()
        throw HttpStatusException(message, code, request.url.toString())
    }
}

fun IntList.sum(): Int {
    var result = 0
    forEach { value -> result += value }
    return result
}