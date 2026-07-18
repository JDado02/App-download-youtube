package com.jdado.ytaudio

import android.app.Application
import android.content.Context
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YtAudioApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Extract the bundled python + yt-dlp + ffmpeg on a background thread.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@YtAudioApp)
                FFmpeg.getInstance().init(this@YtAudioApp)
                engineReady = true
                // The bundled yt-dlp ages quickly as YouTube changes its API, which
                // causes "Please sign in" / HTTP 400 errors. Pull the newest yt-dlp
                // in the background so downloads keep working out of the box.
                runCatching { updateEngine(this@YtAudioApp) }
            } catch (e: Exception) {
                Log.e("YtAudioApp", "Failed to initialize YoutubeDL", e)
                engineError = e.message
            }
        }
    }

    companion object {
        @Volatile
        var engineReady: Boolean = false
        @Volatile
        var engineError: String? = null
        @Volatile
        var updating: Boolean = false

        /**
         * Downloads the latest yt-dlp (nightly channel = fastest fixes for YouTube
         * breakages). Safe to call repeatedly. Returns a human-readable status.
         */
        suspend fun updateEngine(context: Context): String = withContext(Dispatchers.IO) {
            updating = true
            try {
                val status = YoutubeDL.getInstance()
                    .updateYoutubeDL(context, YoutubeDL.UpdateChannel.NIGHTLY)
                when (status) {
                    YoutubeDL.UpdateStatus.DONE -> "yt-dlp aggiornato all'ultima versione"
                    YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "yt-dlp già aggiornato"
                    else -> "Aggiornamento completato"
                }
            } catch (e: Exception) {
                Log.e("YtAudioApp", "yt-dlp update failed", e)
                "Aggiornamento non riuscito: ${e.message}"
            } finally {
                updating = false
            }
        }
    }
}
