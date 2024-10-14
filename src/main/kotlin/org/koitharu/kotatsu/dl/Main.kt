package org.koitharu.kotatsu.dl

import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import org.koitharu.kotatsu.dl.download.DownloadFormat
import org.koitharu.kotatsu.dl.download.MangaDownloader
import org.koitharu.kotatsu.dl.parsers.MangaLoaderContextImpl
import org.koitharu.kotatsu.dl.ui.askSelectBranch
import org.koitharu.kotatsu.dl.util.AppCommand
import org.koitharu.kotatsu.dl.util.ChaptersRange
import org.koitharu.kotatsu.dl.util.colored
import java.io.File

class Main : AppCommand(name = "kotatsu-dl") {

    private val link: String by argument()
    private val destination: File? by option(
        names = arrayOf("--dest", "--destination"),
        help = "Output file or directory path",
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

    override suspend fun invoke(): Int {
        val context = MangaLoaderContextImpl()
        val linkResolver = context.newLinkResolver(link)
        print("Resolving link...")
        val source = linkResolver.getSource()
        if (source == null) {
            println()
            System.err.println("Unsupported manga source")
            return 1
        }
        println('\r')
        colored {
            print("Source: ".cyan)
            print(source.title.bold)
            println()
        }
        val manga = linkResolver.getManga()
        if (manga == null) {
            System.err.println("Manga not found")
            return 1
        }
        colored {
            print("Title: ".cyan)
            println(manga.title.bold)
        }
        var chapters = manga.chapters
        if (chapters.isNullOrEmpty()) {
            System.err.println("Manga contains no chapters")
            throw ProgramResult(1)
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
            ChaptersRange.parse(readLine())
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
        )
        val file = downloader.download()
        colored {
            print("Done.".green.bold)
            print(" Saved to ")
            println(file.absolutePath.bold)
        }
        return 0
    }
}

suspend fun main(args: Array<String>) = Main().main(args)