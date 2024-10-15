package org.koitharu.kotatsu.dl.download

import androidx.collection.MutableIntList
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okio.IOException
import okio.buffer
import okio.sink
import org.koitharu.kotatsu.dl.util.*
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.exception.TooManyRequestExceptions
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.requireBody
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class MangaDownloader(
    private val context: MangaLoaderContext,
    private val manga: Manga,
    private val chapters: List<MangaChapter>,
    private val destination: File?,
    private val chaptersRange: ChaptersRange,
    private val format: DownloadFormat?,
    private val throttle: Boolean,
    private val verbose: Boolean,
    private val parallelism: Int,
) {

    private val progressBarStyle = ProgressBarStyle.builder()
        .rightBracket("]")
        .leftBracket("[")
        .colorCode(if (ColoredConsole.isSupported()) ColoredConsole.BRIGHT_YELLOW.toByte() else 0)
        .block('#')
        .build()

    suspend fun download(): File {
        val output = LocalMangaOutput.create(destination ?: File("").absoluteFile, manga, format)
        if (verbose) {
            colored {
                print("Output: ".cyan)
                println(output.rootFile.canonicalPath)
            }
        }
        val progressBar = ProgressBarBuilder()
            .setStyle(progressBarStyle)
            .setTaskName("Downloading")
            .clearDisplayOnFinish()
            .build()
        progressBar.setExtraMessage("Preparing…")
        val tempDir = Files.createTempDirectory("kdl_").toFile()
        val counters = MutableIntList()
        val totalChapters = chaptersRange.size(chapters)
        try {
            val parser = context.newParserInstance(manga.source as MangaParserSource)
            val coverUrl = manga.largeCoverUrl.ifNullOrEmpty { manga.coverUrl }
            if (coverUrl.isNotEmpty()) {
                downloadFile(coverUrl, tempDir, parser.source).let { file ->
                    output.addCover(file, getFileExtensionFromUrl(coverUrl).orEmpty())
                    file.delete()
                }
            }
            val semaphore = Semaphore(parallelism)
            for (chapter in chapters.withIndex()) {
                progressBar.setExtraMessage(chapter.value.name)
                if (chapter.index !in chaptersRange) {
                    continue
                }
                val pages = runFailsafe(progressBar) { parser.getPages(chapter.value) }
                counters.add(pages.size)
                progressBar.maxHint(counters.sum().toLong() + (totalChapters - counters.size) * pages.size)
                coroutineScope {
                    pages.mapIndexed { pageIndex, page ->
                        launch {
                            semaphore.withPermit {
                                runFailsafe(progressBar) {
                                    val url = parser.getPageUrl(page)
                                    val file = downloadFile(url, tempDir, parser.source)
                                    output.addPage(
                                        chapter = chapter,
                                        file = file,
                                        pageNumber = pageIndex,
                                        ext = getFileExtensionFromUrl(url).orEmpty(),
                                    )
                                    progressBar.step()
                                    if (file.extension == "tmp") {
                                        file.delete()
                                    }
                                }
                            }
                        }
                    }.joinAll()
                }
                output.flushChapter(chapter.value)
            }
            progressBar.setExtraMessage("Finalizing…")
            output.mergeWithExisting()
            output.finish()
            progressBar.close()
            return output.rootFile.canonicalFile
        } catch (e: Throwable) {
            progressBar.close()
            if (e is CancellationException) {
                colored {
                    println()
                    println("Interrupted by user".red)
                }
            }
            throw e
        } finally {
            withContext(NonCancellable) {
                output.cleanup()
                output.closeQuietly()
                tempDir.deleteRecursively()
            }
        }
    }

    private suspend fun <T> runFailsafe(progressBar: ProgressBar, block: suspend () -> T): T {
        var countDown = MAX_FAILSAFE_ATTEMPTS
        failsafe@ while (true) {
            try {
                return block()
            } catch (e: IOException) {
                val retryDelay = if (e is TooManyRequestExceptions) {
                    e.getRetryDelay()
                } else {
                    DOWNLOAD_ERROR_DELAY
                }
                if (countDown <= 0 || retryDelay < 0 || retryDelay > MAX_RETRY_DELAY) {
                    throw e
                } else {
                    countDown--
                    progressBar.pause()
                    try {
                        delay(retryDelay)
                    } finally {
                        progressBar.resume()
                    }
                }
            }
        }
    }

    private suspend fun downloadFile(
        url: String,
        destination: File,
        source: MangaSource,
    ): File {
        if (throttle) {
            slowdownDispatcher.delay(source)
        }
        val request = Request.Builder()
            .url(url)
            .get()
            .header(CommonHeaders.ACCEPT, "image/webp,image/png;q=0.9,image/jpeg,*/*;q=0.8")
            .cacheControl(CommonHeaders.CACHE_CONTROL_NO_STORE)
            .tag(MangaSource::class.java, source)
            .build()
        return context.httpClient.newCall(request).await()
            .ensureSuccess()
            .use { response ->
                val file = File(destination, UUID.randomUUID().toString() + ".tmp")
                try {
                    response.requireBody().use { body ->
                        file.sink(append = false).buffer().use {
                            it.writeAll(body.source())
                        }
                    }
                } catch (e: CancellationException) {
                    file.delete()
                    throw e
                }
                file
            }
    }

    private companion object {

        const val MAX_FAILSAFE_ATTEMPTS = 2
        const val DOWNLOAD_ERROR_DELAY = 2_000L
        const val MAX_RETRY_DELAY = 7_200_000L // 2 hours
        private const val SLOWDOWN_DELAY = 500L

        val slowdownDispatcher = DownloadSlowdownDispatcher(SLOWDOWN_DELAY)
    }
}