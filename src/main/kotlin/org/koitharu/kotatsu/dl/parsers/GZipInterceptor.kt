package org.koitharu.kotatsu.dl.parsers

import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException
import org.koitharu.kotatsu.dl.util.CommonHeaders.CONTENT_ENCODING

class GZipInterceptor : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val newRequest = chain.request().newBuilder()
		newRequest.addHeader(CONTENT_ENCODING, "gzip")
		return try {
			chain.proceed(newRequest.build())
		} catch (e: NullPointerException) {
			throw IOException(e)
		}
	}
}
