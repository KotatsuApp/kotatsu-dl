package org.koitharu.kotatsu.dl.parsers

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.koitharu.kotatsu.dl.util.component6
import org.koitharu.kotatsu.dl.util.component7
import org.koitharu.kotatsu.parsers.util.insertCookie
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

class InMemoryCookieJar : CookieJar {

	private val cache = HashMap<CookieKey, Cookie>()

	override fun loadForRequest(url: HttpUrl): List<Cookie> {
		val time = System.currentTimeMillis()
		return cache.values.filter { it.matches(url) && it.expiresAt >= time }
	}

	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
		cookies.forEach {
			val key = CookieKey(url.host, it.name)
			cache[key] = it
		}
	}

	fun loadFromStream(stream: InputStream) {
		val reader = stream.bufferedReader()
		for (line in reader.lineSequence()) {
			if (line.isBlank() || line.startsWith("# ")) {
				continue
			}
			val (host, _, path, secure, expire, name, value) = line.split(Regex("\\s+"))
			val domain = host.removePrefix("#HttpOnly_").trimStart('.')
			val httpOnly = host.startsWith("#HttpOnly_")
			val cookie = Cookie.Builder()
			cookie.domain(domain)
			if (httpOnly) cookie.httpOnly()
			cookie.path(path)
			if (secure.lowercase(Locale.ROOT).toBooleanStrict()) cookie.secure()
			cookie.expiresAt(TimeUnit.SECONDS.toMillis(expire.toLong()))
			cookie.name(name)
			cookie.value(value)
			insertCookie(domain, cookie.build())
		}
	}

	private data class CookieKey(
		val host: String,
		val name: String,
	)
}