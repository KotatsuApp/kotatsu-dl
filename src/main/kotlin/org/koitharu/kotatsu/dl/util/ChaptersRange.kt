package org.koitharu.kotatsu.dl.util

import org.koitharu.kotatsu.parsers.model.MangaChapter

class ChaptersRange private constructor(private val delegate: Set<Int>?) {

    operator fun contains(index: Int) = delegate == null || delegate.contains(index + 1)

    fun validate() {
        delegate?.forEach { index ->
            require(index > 0) { "Chapter indices must be a positive numbers" }
        }
    }

    fun size(chapters: List<MangaChapter>): Int {
        if (delegate == null) {
            return chapters.size
        }
        return delegate.size // TODO check range
    }

    companion object {

        fun all() = ChaptersRange(null)

        fun parse(str: String?): ChaptersRange {
            if (str.isNullOrBlank() || str == "all") {
                return ChaptersRange(null)
            }
            val result = HashSet<Int>()
            val ranges = str.trim().split(',')
            for (range in ranges) {
                if (range.contains('-')) {
                    val parts = range.split('-')
                    require(parts.size == 2) { "Invalid range $range" }
                    result.addAll(parts[0].trim().toInt()..parts[1].trim().toInt())
                } else {
                    range.split(' ').forEach { part ->
                        result.add(part.trim().toInt())
                    }
                }
            }
            return ChaptersRange(result)
        }
    }
}