package com.jdado.ytaudio

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onSaved: () -> Boolean,
    onLogout: () -> Unit,
    onClose: () -> Unit,
    isLoggedIn: Boolean,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accedi (Google / YouTube)") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Chiudi")
                    }
                },
                actions = {
                    if (isLoggedIn) {
                        TextButton(onClick = {
                            onLogout()
                            Toast.makeText(context, "Logout eseguito", Toast.LENGTH_SHORT).show()
                        }) { Text("Esci") }
                    }
                    TextButton(onClick = {
                        val ok = onSaved()
                        Toast.makeText(
                            context,
                            if (ok) "Login salvato ✅" else "Accedi prima di salvare",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }) { Text("Salva login") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Accedi con il tuo account Google/YouTube, poi premi \"Salva login\". " +
                    "I cookie vengono usati solo in locale per autorizzare i download " +
                    "(video con restrizioni di età o riservati).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> buildLoginWebView(ctx) },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun buildLoginWebView(ctx: android.content.Context): WebView {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    return WebView(ctx).apply {
        cookieManager.setAcceptThirdPartyCookies(this, true)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // A current mobile Chrome UA reduces Google's "browser not secure" refusal.
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
        }
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                cookieManager.flush()
            }
        }
        loadUrl("https://accounts.google.com/ServiceLogin?continue=https://m.youtube.com/")
    }
}
