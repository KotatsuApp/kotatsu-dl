package org.koitharu.kotatsu.dl

import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.dl.download.DownloadFormat
import org.koitharu.kotatsu.dl.download.MangaDownloader
import org.koitharu.kotatsu.dl.parsers.MangaLoaderContextImpl
import org.koitharu.kotatsu.dl.ui.askSelectBranch
import org.koitharu.kotatsu.dl.ui.printSourcesList
import org.koitharu.kotatsu.dl.util.AppCommand
import org.koitharu.kotatsu.dl.util.ChaptersRange
import org.koitharu.kotatsu.dl.util.colored
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import java.io.File

class Main : AppCommand(name = "kotatsu-dl") {

    private val link: String by argument(
        name = "link",
        help = "Direct link to the manga copied from browser as is",
    )
    private val destination: File? by option(
        names = arrayOf("--dest", "--destination"),
        help = "Output file or directory path. Default is current directory",
    ).convert {
        it.replaceFirst(Regex("^~"), System.getProperty("user.home"))
    }.file(
        mustExist = false,
        canBeFile = true,
        canBeDir = true,
    )
    private val format: DownloadFormat? by option(
        names = arrayOf("--format"),
        help = "Output format"
    ).enum<DownloadFormat>(
        ignoreCase = true,
        key = { it.name.lowercase() },
    )
    private val parallelism: Int by option(
        names = arrayOf("-j", "--jobs"),
        help = "Number of parallel jobs for downloading",
    ).int().default(4).check("Jobs count should be between 1 and 10") {
        it in 1..10
    }
    private val throttle: Boolean by option(
        names = arrayOf("--throttle"),
        help = "Slow down downloading to avoid blocking your IP address by server",
    ).flag(default = false)
    private val chaptersRange: ChaptersRange? by option(
        names = arrayOf("--chapters"),
        metavar = "<numbers or range>",
        help = "Numbers of chapters to download. Can be a single numbers or range, e.g. \"1-4,8,11\" or \"all\"",
    ).convert {
        ChaptersRange.parse(it)
    }.validate { range -> range.validate() }
    private val verbose: Boolean by option(
        names = arrayOf("-v", "--verbose"),
        help = "Show more information"
    ).flag(default = false)

    init {
        eagerOption(
            names = arrayOf("--sources"),
            help = "Show list of supported manga sources and exit"
        ) {
            MangaLoaderContextImpl().printSourcesList()
            throw ProgramResult(0)
        }
    }

    override suspend fun invoke(): Int {
        val context = MangaLoaderContextImpl()
        val linkResolver = context.newLinkResolver(link)
        print("Resolving link…")
        val source = linkResolver.getSource()
        val manga = linkResolver.getManga()
        if (source == null || source == MangaParserSource.DUMMY) {
            println()
            error("Unsupported manga source")
        }
        println('\r')
        colored {
            print("Source: ".cyan)
            print(source.title.bold)
            println()
        }
        if (manga == null) {
            error("Manga not found")
        }
        colored {
            print("Title: ".cyan)
            println(manga.title.bold)
        }
        var chapters = manga.chapters
        if (chapters.isNullOrEmpty()) {
            error("Manga contains no chapters")
        }
        chapters = askSelectBranch(chapters)
        colored {
            print("Total chapters: ".cyan)
            println(chapters.size.bold)
        }
        val range = chaptersRange ?: if (chapters.size > 1) {
            colored {
                print("==>".green)
                println(" Chapters to download (e.g. \"1-4,8,11\" or empty for all):")
                print("==>".green)
                print(' ')
            }
            ChaptersRange.parse(readlnOrNull())
        } else {
            ChaptersRange.all()
        }
        val downloader = MangaDownloader(
            context = context,
            manga = manga,
            chapters = chapters,
            destination = destination,
            chaptersRange = range,
            format = format,
            throttle = throttle,
            verbose = verbose,
            parallelism = parallelism,
        )
        val file = withContext(Dispatchers.Default) {
            downloader.download()
        }
        colored {
            print("Done.".green.bold)
            print(" Saved to ")
            println(file.absolutePath.bold)
        }
        return 0
    }
}

suspend fun main(args: Array<String>) = Main().main(args)