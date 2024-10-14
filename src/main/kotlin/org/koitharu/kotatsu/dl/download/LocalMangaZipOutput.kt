package org.koitharu.kotatsu.dl.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import java.io.File
import java.util.zip.ZipFile

class LocalMangaZipOutput(
    rootFile: File,
    manga: Manga,
) : LocalMangaOutput(rootFile) {

    private val output = ZipOutput(File(rootFile.path + ".tmp"))
    private val index = MangaIndex(null)
    private val mutex = Mutex()

    init {
        index.setMangaInfo(manga)
    }

    override suspend fun mergeWithExisting() = mutex.withLock {
        if (rootFile.exists()) {
            runInterruptible(Dispatchers.IO) {
                mergeWith(rootFile)
            }
        }
    }

    override suspend fun addCover(file: File, ext: String) = mutex.withLock {
        val name = buildString {
            append(FILENAME_PATTERN.format(0, 0, 0))
            if (ext.isNotEmpty() && ext.length <= 4) {
                append('.')
                append(ext)
            }
        }
        runInterruptible(Dispatchers.IO) {
            output.put(name, file)
        }
        index.setCoverEntry(name)
    }

    override suspend fun addPage(chapter: IndexedValue<MangaChapter>, file: File, pageNumber: Int, ext: String) =
        mutex.withLock {
            val name = buildString {
                append(FILENAME_PATTERN.format(chapter.value.branch.hashCode(), chapter.index + 1, pageNumber))
                if (ext.isNotEmpty() && ext.length <= 4) {
                    append('.')
                    append(ext)
                }
            }
            runInterruptible(Dispatchers.IO) {
                output.put(name, file)
            }
            index.addChapter(chapter, null)
        }

    override suspend fun flushChapter(chapter: MangaChapter): Boolean = false

    override suspend fun finish() = mutex.withLock {
        runInterruptible(Dispatchers.IO) {
            output.use { output ->
                output.put(ENTRY_NAME_INDEX, index.toString())
                output.finish()
            }
        }
        rootFile.delete()
        output.file.renameTo(rootFile)
        Unit
    }

    override suspend fun cleanup() = mutex.withLock {
        output.file.delete()
        Unit
    }

    override fun close() {
        output.close()
    }

    private fun mergeWith(other: File) {
        var otherIndex: MangaIndex? = null
        ZipFile(other).use { zip ->
            for (entry in zip.entries()) {
                if (entry.name == ENTRY_NAME_INDEX) {
                    otherIndex = MangaIndex(
                        zip.getInputStream(entry).use {
                            it.reader().readText()
                        },
                    )
                } else {
                    output.copyEntryFrom(zip, entry)
                }
            }
        }
        otherIndex?.getMangaInfo()?.chapters?.withIndex()?.let { chapters ->
            for (chapter in chapters) {
                index.addChapter(chapter, null)
            }
        }
    }

	private companion object {

        const val FILENAME_PATTERN = "%08d_%03d%03d"
    }
}
