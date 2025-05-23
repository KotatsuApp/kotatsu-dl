package org.koitharu.kotatsu.dl.download

import okio.Closeable
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.name

class ZipOutput(
	val file: File,
	compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
) : Closeable {

	private val entryNames = HashSet<String>()
	private val isClosed = AtomicBoolean(false)
	private val output = ZipOutputStream(file.outputStream()).apply {
		setLevel(compressionLevel)
		// FIXME: Deflater has been closed
	}

	fun put(name: String, file: File): Boolean {
		return output.appendFile(file, name)
	}

	fun put(name: String, content: String): Boolean {
		return output.appendText(content, name)
	}

	fun addDirectory(name: String): Boolean {
		val entry = if (name.endsWith("/")) {
			ZipEntry(name)
		} else {
			ZipEntry("$name/")
		}
		return if (entryNames.add(entry.name)) {
			output.putNextEntry(entry)
			output.closeEntry()
			true
		} else {
			false
		}
	}

	fun copyEntryFrom(other: ZipFile, entry: ZipEntry): Boolean {
		return if (entryNames.add(entry.name)) {
			val zipEntry = ZipEntry(entry.name)
			output.putNextEntry(zipEntry)
			try {
				other.getInputStream(entry).use { input ->
					input.copyTo(output)
				}
			} finally {
				output.closeEntry()
			}
			true
		} else {
			false
		}
	}

	fun finish() {
		output.finish()
		output.flush()
	}

	override fun close() {
		if (isClosed.compareAndSet(false, true)) {
			output.close()
		}
	}

	private fun ZipOutputStream.appendFile(fileToZip: File, name: String): Boolean {
		if (fileToZip.isDirectory) {
			val entry = if (name.endsWith("/")) {
				ZipEntry(name)
			} else {
				ZipEntry("$name/")
			}
			if (!entryNames.add(entry.name)) {
				return false
			}
			putNextEntry(entry)
			closeEntry()
			Files.newDirectoryStream(fileToZip.toPath()).use {
				it.forEach { childFile ->
					appendFile(childFile.toFile(), "$name/${childFile.name}")
				}
			}
		} else {
			FileInputStream(fileToZip).use { fis ->
				if (!entryNames.add(name)) {
					return false
				}
				val zipEntry = ZipEntry(name)
				putNextEntry(zipEntry)
				try {
					fis.copyTo(this)
				} finally {
					closeEntry()
				}
			}
		}
		return true
	}

	private fun ZipOutputStream.appendText(content: String, name: String): Boolean {
		if (!entryNames.add(name)) {
			return false
		}
		val zipEntry = ZipEntry(name)
		putNextEntry(zipEntry)
		try {
			content.byteInputStream().copyTo(this)
		} finally {
			closeEntry()
		}
		return true
	}
}
