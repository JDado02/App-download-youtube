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
    data class Working(val percent: Float, val line: String, val itemId: String?) : DownloadState
    data class Done(val title: String, val savedAs: String) : DownloadState
    data class Error(val message: String) : DownloadState
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Results(val items: List<SearchItem>) : SearchState
    data object Empty : SearchState
    data class Error(val message: String) : SearchState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    // ---- Navigation ----
    private val _tab = MutableStateFlow(0) // 0 = Cerca, 1 = Link
    val tab = _tab.asStateFlow()
    fun onTabChange(index: Int) { _tab.value = index }

    // ---- Link download ----
    private val _url = MutableStateFlow("")
    val url = _url.asStateFlow()
    fun onUrlChange(value: String) { _url.value = value }

    // ---- Search ----
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    fun onQueryChange(value: String) { _query.value = value }

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState = _searchState.asStateFlow()

    // ---- Shared download state ----
    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download = _download.asStateFlow()

    // ---- Engine / login ----
    private val _engineNote = MutableStateFlow<String?>(null)
    val engineNote = _engineNote.asStateFlow()

    private val _loggedIn = MutableStateFlow(CookieStore.isLoggedIn(getApplication()))
    val loggedIn = _loggedIn.asStateFlow()

    private var downloadJob: Job? = null
    private var searchJob: Job? = null

    val isDownloading: Boolean get() = _download.value is DownloadState.Working

    fun refreshLoginState() {
        _loggedIn.value = CookieStore.isLoggedIn(getApplication())
    }

    fun onLoginSaved(): Boolean {
        val ok = CookieStore.exportFromWebView(getApplication())
        refreshLoginState()
        return ok
    }

    fun logout() {
        CookieStore.logout(getApplication())
        refreshLoginState()
    }

    fun runSearch() {
        val q = _query.value.trim()
        if (q.isBlank()) return
        if (!ensureEngine()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                val items = YtSearch.search(getApplication(), q)
                _searchState.value =
                    if (items.isEmpty()) SearchState.Empty else SearchState.Results(items)
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "Ricerca non riuscita")
            }
        }
    }

    fun startLinkDownload() {
        val target = _url.value.trim()
        if (target.isBlank()) {
            _download.value = DownloadState.Error("Incolla prima un link di YouTube")
            return
        }
        download(target, itemId = null)
    }

    fun downloadItem(item: SearchItem) = download(item.watchUrl, itemId = item.id)

    private fun download(target: String, itemId: String?) {
        if (isDownloading) return
        if (!ensureEngine()) return
        downloadJob = viewModelScope.launch {
            _download.value = DownloadState.Working(0f, "Avvio…", itemId)
            try {
                val result = AudioDownloader.download(getApplication(), target) { percent, line ->
                    _download.value = DownloadState.Working(percent, line, itemId)
                }
                _download.value = DownloadState.Done(result.title, result.savedAs)
            } catch (e: Exception) {
                _download.value = DownloadState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    fun cancelDownload() {
        AudioDownloader.cancel()
        downloadJob?.cancel()
        _download.value = DownloadState.Idle
    }

    fun dismissDownload() { _download.value = DownloadState.Idle }

    fun updateEngine() {
        if (YtAudioApp.updating) return
        viewModelScope.launch {
            _engineNote.value = "Aggiornamento di yt-dlp in corso…"
            _engineNote.value = YtAudioApp.updateEngine(getApplication())
        }
    }

    private fun ensureEngine(): Boolean {
        if (!YtAudioApp.engineReady) {
            _download.value = DownloadState.Error(
                YtAudioApp.engineError
                    ?: "Motore non ancora pronto, riprova tra qualche secondo"
            )
            return false
        }
        return true
    }
}
