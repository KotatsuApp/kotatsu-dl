package org.koitharu.kotatsu.dl.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okio.Closeable
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.toFileNameSafe
import java.io.File

sealed class LocalMangaOutput(
    val rootFile: File,
) : Closeable {

    abstract suspend fun mergeWithExisting()

    abstract suspend fun addCover(file: File, ext: String)

    abstract suspend fun addPage(chapter: IndexedValue<MangaChapter>, file: File, pageNumber: Int, ext: String)

    abstract suspend fun flushChapter(chapter: MangaChapter): Boolean

    abstract suspend fun finish()

    abstract suspend fun cleanup()

    companion object {

        const val ENTRY_NAME_INDEX = "index.json"
        const val SUFFIX_TMP = ".tmp"

        suspend fun create(
            target: File,
            manga: Manga,
            format: DownloadFormat?,
        ): LocalMangaOutput = runInterruptible(Dispatchers.IO) {
            val targetFormat = format ?: if (manga.chapters.let { it != null && it.size <= 3 }) {
                DownloadFormat.CBZ
            } else {
                DownloadFormat.DIR
            }
            var file = if (target.isDirectory || (!target.exists() && targetFormat == DownloadFormat.DIR)) {
                if (!target.exists()) {
                    target.mkdirs()
                }
                val baseName = manga.title.toFileNameSafe()
                when (targetFormat) {
                    DownloadFormat.CBZ -> File(target, "$baseName.cbz")
                    DownloadFormat.ZIP -> File(target, "$baseName.zip")
                    DownloadFormat.DIR -> File(target, baseName)
                }
            } else {
                target.parentFile?.run {
                    if (!exists()) mkdirs()
                }
                target
            }
            getNextAvailable(file, manga)
        }

        private fun getNextAvailable(
            file: File,
            manga: Manga,
        ): LocalMangaOutput {
            var i = 0
            val baseName = file.nameWithoutExtension
            val ext = file.extension.let { if (it.isNotEmpty()) ".$it" else "" }
            while (true) {
                val fileName = (if (i == 0) baseName else baseName + "_$i") + ext
                val target = File(file.parentFile, fileName)
                if (target.exists()) {
                    i++
                } else {
                    return when {
                        target.isDirectory -> LocalMangaDirOutput(target, manga)
                        else -> LocalMangaZipOutput(target, manga)
                    }
                }
            }
        }
    }
}
