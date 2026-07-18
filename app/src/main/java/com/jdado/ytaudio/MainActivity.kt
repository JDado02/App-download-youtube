package com.jdado.ytaudio

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                MainScreen(vm)
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
                Regex("https?://\\S+").find(shared)?.value?.let { viewModel.onUrlChange(it) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val url by vm.url.collectAsState()
    val state by vm.state.collectAsState()
    val engineNote by vm.engineNote.collectAsState()
    val working = state is DownloadState.Working

    Scaffold(
        topBar = { TopAppBar(title = { Text("YT Audio → MP3") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.height(64.dp),
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

            if (working) {
                OutlinedButton(onClick = vm::cancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Annulla")
                }
            } else {
                Button(onClick = vm::start, modifier = Modifier.fillMaxWidth()) {
                    Text("Scarica audio (MP3 max qualità)")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = vm::updateEngine,
                enabled = !working,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Aggiorna motore (yt-dlp)")
            }
            engineNote?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
            StatusCard(state)

            Spacer(Modifier.height(24.dp))
            Text(
                text = "Scarica solo contenuti di cui detieni i diritti o consentiti dalla " +
                    "licenza. Rispetta i Termini di Servizio di YouTube e le leggi sul copyright.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusCard(state: DownloadState) {
    when (state) {
        is DownloadState.Idle -> Unit
        is DownloadState.Working -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row2("Download in corso…", "${state.percent.toInt()}%")
                Spacer(Modifier.height(8.dp))
                if (state.percent > 0f) {
                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    CircularProgressIndicator()
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    state.line,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        is DownloadState.Done -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("✅ Completato", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(state.title, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Salvato in: ${state.savedAs}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is DownloadState.Error -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("⚠️ Errore", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                if (state.message.contains("sign in", ignoreCase = true) ||
                    state.message.contains("400") ||
                    state.message.contains("Precondition", ignoreCase = true)
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Suggerimento: YouTube ha bloccato la versione attuale di yt-dlp. " +
                            "Premi \"Aggiorna motore (yt-dlp)\" e riprova.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Row2(left: String, right: String) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(left, style = MaterialTheme.typography.titleMedium)
        Text(right, style = MaterialTheme.typography.titleMedium)
    }
}
