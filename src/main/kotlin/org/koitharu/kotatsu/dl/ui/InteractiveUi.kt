package org.koitharu.kotatsu.dl.ui

import me.tongfei.progressbar.TerminalUtils
import org.koitharu.kotatsu.dl.util.colored
import org.koitharu.kotatsu.dl.util.ifNullOrEmpty
import org.koitharu.kotatsu.parsers.model.MangaChapter
import java.util.*

fun askSelectBranch(chapters: List<MangaChapter>): List<MangaChapter> {
    val branches = chapters.groupBy { it.branch }
        .toList()
        .sortedWith(compareBy<Pair<String?, List<MangaChapter>>> { weightOf(it.first) }.thenByDescending { it.second.size })
    if (branches.size > 1) {
        colored {
            println("Available translations: ".cyan)
            branches.forEachIndexed { index, entry ->
                print("${index + 1}.".purple.bold)
                print(' ')
                print((entry.first ?: "Unknown").bold)
                println(" (${entry.second.size})".bright)
            }
            print("==>".green)
            println(" Select translation (default 1):")
            print("==>".green)
            print(' ')
        }
        val userInput = readlnOrNull()?.trim().ifNullOrEmpty { "1" }
        val branch = branches[userInput.toInt() - 1].first
        return chapters.filter { chapter -> chapter.branch == branch }
    } else {
        return chapters
    }
}

private fun getTerminalWidth(): Int = runCatching {
    val function = TerminalUtils::class.java.getDeclaredMethod("getTerminalWidth")
    return function.invoke(null) as Int
}.getOrDefault(0)

private fun weightOf(value: String?) = if (value != null) {
    val locale = Locale.getDefault()
    val displayLanguage = locale.getDisplayLanguage(locale)
    val displayName = locale.getDisplayName(locale)
    if (value.contains(displayName, ignoreCase = true) || value.contains(displayLanguage, ignoreCase = true)) {
        0
    } else {
        2
    }
} else {
    1
}