package com.jdado.ytaudio

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YtAudioApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Extract the bundled python + yt-dlp + ffmpeg on a background thread.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@YtAudioApp)
                FFmpeg.getInstance().init(this@YtAudioApp)
                engineReady = true
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
    }
}
