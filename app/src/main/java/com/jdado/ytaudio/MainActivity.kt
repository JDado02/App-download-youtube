package com.jdado.ytaudio

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jdado.ytaudio.ui.theme.YTAudioTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        handleSharedText(intent)

        setContent {
            YTAudioTheme {
                val vm: MainViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedText(intent)
    }

    private fun handleSharedText(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { shared ->
                Regex("https?://\\S+").find(shared)?.value?.let {
                    viewModel.onUrlChange(it)
                    viewModel.onTabChange(1)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel) {
    var showLogin by remember { mutableStateOf(false) }
    val tab by vm.tab.collectAsState()
    val download by vm.download.collectAsState()
    val loggedIn by vm.loggedIn.collectAsState()
    val engineNote by vm.engineNote.collectAsState()

    if (showLogin) {
        LoginScreen(
            onSaved = {
                val ok = vm.onLoginSaved()
                if (ok) showLogin = false
                ok
            },
            onLogout = { vm.logout() },
            onClose = { showLogin = false },
            isLoggedIn = loggedIn,
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YT Davide") },
                actions = {
                    IconButton(onClick = vm::updateEngine) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna motore")
                    }
                    IconButton(onClick = { showLogin = true }) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = "Account",
                            tint = if (loggedIn) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { vm.onTabChange(0) },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("Cerca") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { vm.onTabChange(1) },
                    icon = { Icon(Icons.Filled.Link, contentDescription = null) },
                    label = { Text("Link") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            engineNote?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Box(Modifier.weight(1f)) {
                if (tab == 0) SearchTab(vm, download) else LinkTab(vm)
            }
            DownloadStatus(download, onCancel = vm::cancelDownload, onDismiss = vm::dismissDownload)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTab(vm: MainViewModel, download: DownloadState) {
    val query by vm.query.collectAsState()
    val state by vm.searchState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = vm::onQueryChange,
            label = { Text("Cerca su YouTube") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = vm::runSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Cerca")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.runSearch() }),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (val s = state) {
                is SearchState.Idle -> Centered("Cerca un brano o un video per nome 🎧")
                is SearchState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                is SearchState.Empty -> Centered("Nessun risultato")
                is SearchState.Error -> Centered("⚠️ ${s.message}")
                is SearchState.Results -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    items(s.items, key = { it.id }) { item ->
                        val downloadingThis = (download as? DownloadState.Working)?.itemId == item.id
                        val percent = (download as? DownloadState.Working)?.percent ?: 0f
                        SearchRow(
                            item = item,
                            downloading = downloadingThis,
                            percent = percent,
                            enabled = download !is DownloadState.Working,
                            onDownload = { vm.downloadItem(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(
    item: SearchItem,
    downloading: Boolean,
    percent: Float,
    enabled: Boolean,
    onDownload: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(120.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                if (item.durationText.isNotBlank()) {
                    Text(
                        item.durationText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.uploader.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.uploader,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (downloading) {
                Box(Modifier.size(44.dp), Alignment.Center) {
                    if (percent > 0f) {
                        CircularProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier.size(36.dp),
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                }
            } else {
                IconButton(onClick = onDownload, enabled = enabled) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "Scarica MP3",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkTab(vm: MainViewModel) {
    val url by vm.url.collectAsState()
    val download by vm.download.collectAsState()
    val working = download is DownloadState.Working

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = url,
            onValueChange = vm::onUrlChange,
            label = { Text("Link di YouTube") },
            singleLine = true,
            enabled = !working,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = vm::startLinkDownload,
            enabled = !working,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Scarica audio (MP3 max qualità)")
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Scarica solo contenuti di cui detieni i diritti o consentiti dalla licenza. " +
                "Rispetta i Termini di Servizio di YouTube e le leggi sul copyright.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DownloadStatus(state: DownloadState, onCancel: () -> Unit, onDismiss: () -> Unit) {
    when (state) {
        is DownloadState.Idle -> Unit
        is DownloadState.Working -> Card(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Download… ${state.percent.toInt()}%", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onCancel) { Text("Annulla") }
                }
                Spacer(Modifier.height(6.dp))
                if (state.percent > 0f) {
                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    state.line,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        is DownloadState.Done -> Card(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("✅ Completato", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onDismiss) { Text("OK") }
                }
                Text(state.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Salvato in: ${state.savedAs}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is DownloadState.Error -> Card(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("⚠️ Errore", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onDismiss) { Text("Chiudi") }
                }
                Text(state.message, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (state.message.contains("sign in", true) || state.message.contains("400") ||
                    state.message.contains("Precondition", true)
                ) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Suggerimento: premi ↻ in alto per aggiornare yt-dlp, oppure esegui il " +
                            "login dall'icona account, poi riprova.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Centered(text: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}
