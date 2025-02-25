package org.koitharu.kotatsu.dl.download

import com.github.ajalt.clikt.core.UsageError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okio.Closeable
import org.koitharu.kotatsu.dl.util.getNextAvailable
import org.koitharu.kotatsu.dl.util.toFileNameSafe
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
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
            destination: File,
            manga: Manga,
            preferredFormat: DownloadFormat?,
        ): LocalMangaOutput = runInterruptible(Dispatchers.IO) {
            when {
                // option 0 - destination is a existing file/dir and we should write manga into id directly
                destination.exists() && (destination.isFile || destination.isMangaDir()) -> {
                    TODO("Downloading into existing manga destination is not supported yet")
                }
                // option 1 - destination is an existing directory and we should create a nested dir/file for manga
                destination.exists() && destination.isDirectory -> {
                    val baseName = manga.title.toFileNameSafe()
                    val format = preferredFormat ?: detectFormat(manga)
                    val targetFile = File(
                        destination, when (format) {
                            DownloadFormat.CBZ -> "$baseName.cbz"
                            DownloadFormat.ZIP -> "$baseName.zip"
                            DownloadFormat.DIR -> baseName
                        }
                    )
                    createDirectly(targetFile.getNextAvailable(), manga, format)
                }
                // option 2 - destination is a non-existing file/dir and we should write manga into id directly
                !destination.exists() -> {
                    val parentDir: File? = destination.parentFile
                    parentDir?.mkdirs()
                    createDirectly(destination, manga, preferredFormat ?: detectFormat(destination))
                }

                else -> throw UsageError(
                    message = "Unable to determine destination file or directory. Please specify it explicitly",
                    paramName = "--destination"
                )
            }
        }

        private fun createDirectly(destination: File, manga: Manga, format: DownloadFormat) = when (format) {
            DownloadFormat.CBZ,
            DownloadFormat.ZIP,
                -> LocalMangaZipOutput(destination, manga)

            DownloadFormat.DIR -> LocalMangaDirOutput(destination, manga)
        }

        private fun detectFormat(destination: File): DownloadFormat {
            return when (destination.extension.lowercase()) {
                "cbz" -> return DownloadFormat.CBZ
                "zip" -> return DownloadFormat.ZIP
                "" -> return DownloadFormat.DIR
                else -> throw UsageError(
                    message = "Unable to determine output format. Please specify it explicitly",
                    paramName = "--format"
                )
            }
        }

        private fun detectFormat(manga: Manga): DownloadFormat {
            return if (manga.chapters.let { it != null && it.size <= 5 }) {
                DownloadFormat.CBZ
            } else {
                DownloadFormat.DIR
            }
        }

        private fun File.isMangaDir(): Boolean {
            return list()?.contains("index.json") == true
        }
    }
}
