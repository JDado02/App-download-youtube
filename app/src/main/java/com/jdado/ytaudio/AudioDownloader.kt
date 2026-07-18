package com.jdado.ytaudio

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wraps yt-dlp to extract the best-quality audio track from a video URL and
 * transcode it to MP3, then exposes the file to the device's music library.
 */
object AudioDownloader {

    data class Result(val title: String, val savedAs: String)

    @Volatile
    private var currentProcessId: String? = null

    /** Aborts the running yt-dlp process, if any. */
    fun cancel() {
        currentProcessId?.let { runCatching { YoutubeDL.getInstance().destroyProcessById(it) } }
    }

    /**
     * @param onProgress invoked with a 0..100 percentage and the raw yt-dlp log line.
     */
    suspend fun download(
        context: Context,
        url: String,
        onProgress: (percent: Float, line: String) -> Unit,
    ): Result = withContext(Dispatchers.IO) {
        require(url.isNotBlank()) { "URL vuoto" }
        val processId = "dl-${System.currentTimeMillis()}"
        currentProcessId = processId

        // Fetch metadata for a clean filename (best effort).
        val title = runCatching { YoutubeDL.getInstance().getInfo(url).title }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "audio"

        // Isolated temp dir so we can reliably find the produced file afterwards.
        val tmpDir = File(context.cacheDir, "dl_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val request = YoutubeDLRequest(url).apply {
                addOption("-x")                       // extract audio
                addOption("--audio-format", "mp3")    // container/codec
                addOption("--audio-quality", "0")     // 0 = best VBR quality
                addOption("-f", "bestaudio/best")     // pick the best audio stream
                addOption("--no-playlist")            // single item only
                addOption("--embed-thumbnail")        // cover art when available
                addOption("--add-metadata")           // title/artist tags
                addOption("--no-mtime")
                // Use player clients that don't require a signed-in session / PO token.
                // Works around YouTube's "Please sign in" / HTTP 400 responses to the
                // default web client. yt-dlp tries them in order and falls back.
                addOption("--extractor-args", "youtube:player_client=tv,web_safari,android_vr")
                // Authenticated session for age-restricted / sign-in-required videos.
                if (CookieStore.isLoggedIn(context)) {
                    addOption("--cookies", CookieStore.file(context).absolutePath)
                }
                addOption("-o", File(tmpDir, "%(title)s.%(ext)s").absolutePath)
            }

            YoutubeDL.getInstance().execute(request, processId = processId, callback = { progress, _, line ->
                onProgress(progress.coerceIn(0f, 100f), line)
            })

            val produced = tmpDir.listFiles()?.firstOrNull { it.extension.equals("mp3", true) }
                ?: tmpDir.listFiles()?.maxByOrNull { it.length() }
                ?: throw IllegalStateException("Nessun file audio prodotto")

            val displayName = sanitize(produced.name)
            val savedAs = exportToMusicLibrary(context, produced, displayName)
            Result(title = title, savedAs = savedAs)
        } finally {
            currentProcessId = null
            tmpDir.deleteRecursively()
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(180)

    /** Copies the file into the shared Music collection so it shows up in music apps. */
    private fun exportToMusicLibrary(context: Context, source: File, displayName: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/YTAudio")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Impossibile creare la voce nella libreria")
            resolver.openOutputStream(uri).use { out ->
                source.inputStream().use { it.copyTo(out!!) }
            }
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Music/YTAudio/$displayName"
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "YTAudio"
            ).apply { mkdirs() }
            val dest = File(dir, displayName)
            source.copyTo(dest, overwrite = true)
            dest.absolutePath
        }
    }
}
