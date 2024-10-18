package org.koitharu.kotatsu.dl.util

import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import com.github.ajalt.clikt.core.FileNotFound
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.context
import okio.IOException
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
        val exitCode = try {
            invoke()
        } catch (e: IllegalStateException) {
            throw PrintMessage(e.message.ifNullOrEmpty { GENERIC_ERROR_MSG }, 1, true)
        } catch (e: NotImplementedError) {
            throw PrintMessage(e.message.ifNullOrEmpty { GENERIC_ERROR_MSG }, 2, true)
        }
        throw ProgramResult(exitCode)
    }

    abstract suspend fun invoke(): Int
}