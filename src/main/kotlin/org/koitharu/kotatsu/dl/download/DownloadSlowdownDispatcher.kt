package org.koitharu.kotatsu.dl.download

import androidx.collection.MutableObjectLongMap
import kotlinx.coroutines.delay
import org.koitharu.kotatsu.parsers.model.MangaSource

class DownloadSlowdownDispatcher(
	private val defaultDelay: Long,
) {
	private val timeMap = MutableObjectLongMap<MangaSource>()

	suspend fun delay(source: MangaSource) {
		val lastRequest = synchronized(timeMap) {
			val res = timeMap.getOrDefault(source, 0L)
			timeMap[source] = System.currentTimeMillis()
			res
		}
		if (lastRequest != 0L) {
			delay(lastRequest + defaultDelay - System.currentTimeMillis())
		}
	}
}
