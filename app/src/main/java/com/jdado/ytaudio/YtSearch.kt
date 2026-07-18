package com.jdado.ytaudio

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SearchItem(
    val id: String,
    val title: String,
    val uploader: String,
    val durationText: String,
    val thumbnailUrl: String,
) {
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$id"
}

/** Searches YouTube via yt-dlp's ytsearch and returns lightweight result rows. */
object YtSearch {

    suspend fun search(context: Context, query: String, limit: Int = 20): List<SearchItem> =
        withContext(Dispatchers.IO) {
            require(query.isNotBlank())
            val request = YoutubeDLRequest("ytsearch$limit:$query").apply {
                addOption("--dump-single-json")
                addOption("--flat-playlist")
                addOption("--no-warnings")
                addOption("--extractor-args", "youtube:player_client=tv,web_safari,android_vr")
                if (CookieStore.isLoggedIn(context)) {
                    addOption("--cookies", CookieStore.file(context).absolutePath)
                }
            }
            val response = YoutubeDL.getInstance().execute(request)
            parse(response.out)
        }

    private fun parse(json: String): List<SearchItem> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val entries = root.optJSONArray("entries") ?: return emptyList()
        val out = ArrayList<SearchItem>(entries.length())
        for (i in 0 until entries.length()) {
            val e = entries.optJSONObject(i) ?: continue
            val id = e.optString("id")
            if (id.isBlank()) continue
            val title = e.optString("title").ifBlank { "(senza titolo)" }
            val uploader = e.optString("uploader")
                .ifBlank { e.optString("channel") }
                .ifBlank { "" }
            val duration = formatDuration(e.optDouble("duration", 0.0).toLong())
            out.add(
                SearchItem(
                    id = id,
                    title = title,
                    uploader = uploader,
                    durationText = duration,
                    // Predictable YouTube thumbnail endpoint — always available.
                    thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
                )
            )
        }
        return out
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
