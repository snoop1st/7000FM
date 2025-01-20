package com.snoop.fm7000

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

suspend fun fetchCurrentTrack(): String? {
    return withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect("https://7000fm.gr/").get()
            val trackElement = doc.selectFirst("#current-track")
            trackElement?.text()?.trim()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
