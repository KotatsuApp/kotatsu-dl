package org.koitharu.kotatsu.dl.util

import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import com.github.ajalt.clikt.core.FileNotFound
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.context
import okio.IOException
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import kotlin.io.path.Path
import kotlin.io.path.readText

abstract class AppCommand(name: String) : CoreSuspendingCliktCommand(name) {

    override val printHelpOnEmptyArgs = true

    init {
        context {
            readArgumentFile = {
                try {
                    Path(it).readText()
                } catch (_: IOException) {
                    throw FileNotFound(it)
                }
            }
            readEnvvar = { System.getenv(it) }
            exitProcess = { Runtime.getRuntime().exit(it) }
            echoMessage = { context, message, newline, err ->
                val writer = if (err) System.err else System.out
                if (newline) {
                    writer.println(message)
                } else {
                    writer.print(message)
                }
            }
        }
    }

    final override suspend fun run() {
        val exitCode = runCatchingCancellable {
            invoke()
        }.onFailure { e ->
            System.err.println(e.message)
        }.getOrDefault(1)
        throw ProgramResult(exitCode)
    }

    abstract suspend fun invoke(): Int
}