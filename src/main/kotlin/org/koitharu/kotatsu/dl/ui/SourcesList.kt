package org.koitharu.kotatsu.dl.ui

import org.koitharu.kotatsu.dl.util.colored
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.util.toTitleCase
import java.util.*

fun MangaLoaderContext.printSourcesList() {
    println("List of supported sources:")
    val groups = MangaParserSource.entries.groupBy { it.locale }
    for (group in groups) {
        colored {
            print(" - ")
            println(group.key.toLocaleName().cyan)
        }
        val sources = group.value.sortedBy { it.title }
        for (source in sources) {
            val parser = try {
                newParserInstance(source)
            } catch (_: Throwable) {
                continue
            }
            colored {
                print(source.title.bold)
                print(' ')
                print("https://")
                print(parser.domain)
                print(' ')
                when (source.contentType) {
                    ContentType.HENTAI -> print("[Hentai]".yellow)
                    ContentType.COMICS -> print("[Comics]".yellow)
                    else -> Unit
                }
                if (source.isBroken) {
                    print("[Broken]".red)
                }
                println()
            }
        }
    }
}

private fun String.toLocaleName(): String {
    if (isEmpty()) {
        return "Multilingual"
    }
    val lc = Locale(this)
    return lc.getDisplayName(lc).toTitleCase(lc)
}