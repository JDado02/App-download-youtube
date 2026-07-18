package com.jdado.ytaudio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Working(val percent: Float, val line: String) : DownloadState
    data class Done(val title: String, val savedAs: String) : DownloadState
    data class Error(val message: String) : DownloadState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _url = MutableStateFlow("")
    val url = _url.asStateFlow()

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state = _state.asStateFlow()

    private val _engineNote = MutableStateFlow<String?>(null)
    val engineNote = _engineNote.asStateFlow()

    private var job: Job? = null

    fun updateEngine() {
        if (YtAudioApp.updating) return
        viewModelScope.launch {
            _engineNote.value = "Aggiornamento di yt-dlp in corso…"
            _engineNote.value = YtAudioApp.updateEngine(getApplication())
        }
    }

    fun onUrlChange(value: String) {
        _url.value = value
    }

    fun start() {
        val target = _url.value.trim()
        if (target.isBlank()) {
            _state.value = DownloadState.Error("Incolla prima un link di YouTube")
            return
        }
        if (!YtAudioApp.engineReady) {
            _state.value = DownloadState.Error(
                YtAudioApp.engineError ?: "Motore di download non ancora pronto, riprova tra qualche secondo"
            )
            return
        }
        job = viewModelScope.launch {
            _state.value = DownloadState.Working(0f, "Avvio…")
            try {
                val result = AudioDownloader.download(getApplication(), target) { percent, line ->
                    _state.value = DownloadState.Working(percent, line)
                }
                _state.value = DownloadState.Done(result.title, result.savedAs)
            } catch (e: Exception) {
                _state.value = DownloadState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    fun cancel() {
        job?.cancel()
        _state.value = DownloadState.Idle
    }

    fun reset() {
        _state.value = DownloadState.Idle
    }
}
