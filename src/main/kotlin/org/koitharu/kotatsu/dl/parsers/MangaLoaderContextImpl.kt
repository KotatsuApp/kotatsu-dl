package org.koitharu.kotatsu.dl.parsers

import com.koushikdutta.quack.QuackContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.CookieJar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.network.UserAgents
import org.koitharu.kotatsu.parsers.util.requireBody
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

class MangaLoaderContextImpl : MangaLoaderContext() {

    override val cookieJar: CookieJar = InMemoryCookieJar()

    override val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(CloudFlareInterceptor())
        .addInterceptor(GZipInterceptor())
        .addInterceptor(RateLimitInterceptor())
        .addInterceptor(CommonHeadersInterceptor(this))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Suppress("OVERRIDE_DEPRECATION")
    override suspend fun evaluateJs(script: String): String? = evaluateJs("", script)

    override suspend fun evaluateJs(baseUrl: String, script: String): String? = runInterruptible(Dispatchers.Default) {
        QuackContext.create().use {
            it.evaluate(script)?.toString()
        }
    }

    override fun getConfig(source: MangaSource): MangaSourceConfig = DefaultMangaSourceConfig()

    override fun getDefaultUserAgent(): String = UserAgents.FIREFOX_DESKTOP

    override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response {
        val srcImage = response.requireBody().byteStream().use { ImageIO.read(it) }
        checkNotNull(srcImage) { "Cannot decode image" }
        val resImage = (redraw(BitmapImpl(srcImage)) as BitmapImpl)
        return response.newBuilder()
            .body(resImage.compress("png").toResponseBody("image/png".toMediaTypeOrNull()))
            .build()
    }

    override fun createBitmap(width: Int, height: Int): Bitmap {
        return BitmapImpl(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB))
    }
}