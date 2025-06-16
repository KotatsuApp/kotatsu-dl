package org.koitharu.kotatsu.dl.parsers

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.koitharu.kotatsu.dl.util.CommonHeaders
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.core.AbstractMangaParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mergeWith
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.net.IDN

class CommonHeadersInterceptor(
	private val context: MangaLoaderContext
) : Interceptor {

	override fun intercept(chain: Chain): Response {
		val request = chain.request()
		val source = request.tag(MangaSource::class.java)
		val parser = if (source is MangaParserSource) {
			context.newParserInstance(source)
		} else {
			null
		}
		val sourceHeaders = parser?.getRequestHeaders()
		val headersBuilder = request.headers.newBuilder()
		if (sourceHeaders != null) {
			headersBuilder.mergeWith(sourceHeaders, replaceExisting = false)
		}
		if (headersBuilder[CommonHeaders.USER_AGENT] == null) {
			headersBuilder[CommonHeaders.USER_AGENT] = context.getDefaultUserAgent()
		}

		if (headersBuilder[CommonHeaders.REFERER] == null && parser != null) {
			val domain = (parser as? AbstractMangaParser)?.domain
			if (domain != null) {
				val idn = IDN.toASCII(domain)
				headersBuilder[CommonHeaders.REFERER] = "https://$idn/"
			}
		}

		val newRequest = request.newBuilder().headers(headersBuilder.build()).build()
		return if (parser is Interceptor) {
			parser.interceptSafe(ProxyChain(chain, newRequest))
		} else {
			chain.proceed(newRequest)
		}
	}

	private fun Interceptor.interceptSafe(chain: Chain): Response = runCatchingCancellable {
		intercept(chain)
	}.getOrElse { e ->
		if (e is IOException) {
			throw e
		} else {
			// only IOException can be safely thrown from an Interceptor
			throw IOException("Error in interceptor: ${e.message}", e)
		}
	}

	private class ProxyChain(
		private val originalChain: Chain,
		private val newRequest: Request
	) : Chain by originalChain {
		override fun request(): Request = newRequest
	}
}
