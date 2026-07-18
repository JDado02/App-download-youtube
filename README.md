# YT Davide 🎵

App Android che **cerca video su YouTube per nome** e ne **scarica l'audio in MP3 alla massima qualità** (VBR `--audio-quality 0`) — direttamente nella libreria musicale del dispositivo. Puoi anche incollare un link.

Costruita con **Kotlin + Jetpack Compose**, usa [`youtubedl-android`](https://github.com/JunkFood02/youtubedl-android) (porting di `yt-dlp`) + **FFmpeg** per estrazione/transcodifica e **Coil** per le copertine.

> ⚠️ **Avviso legale.** Scarica esclusivamente contenuti di cui detieni i diritti, di pubblico dominio o consentiti dalla relativa licenza. L'uso deve rispettare i Termini di Servizio di YouTube e le leggi sul copyright. Strumento a solo scopo educativo/uso personale legittimo.

## Funzionalità

- 🔎 **Ricerca in-app** per nome, con **copertine**, canale e durata. Tasto download su ogni risultato: niente più copia-incolla del link.
- 🔗 **Download da link** (incolla o "Condividi → YT Davide").
- 🎧 Estrazione della **migliore traccia audio** → MP3, con tag ID3 (titolo/artista) e copertina incorporati.
- 🔐 **Login Google/YouTube** (WebView): i cookie vengono salvati in locale e passati a yt-dlp (`--cookies`) per scaricare video con **restrizioni di età o riservati**.
- ↻ **Aggiornamento yt-dlp** integrato (auto all'avvio + pulsante manuale) contro i blocchi di YouTube.
- 💾 Salvataggio in `Music/YTAudio/` via MediaStore (nessun permesso storage su Android 10+).

## Come ottenere l'APK

A ogni push la **GitHub Action** (`.github/workflows/build.yml`) compila l'APK:

1. Tab **Actions** del repo → ultimo run "Build APK".
2. Scarica l'artifact **`yt-audio-debug-apk`**.
3. Trasferiscilo sul telefono e installalo (abilita "origini sconosciute").

## Uso

- **Cerca**: digita il nome, premi 🔎, poi l'icona ⬇️ sul risultato.
- **Link**: incolla l'URL e premi scarica.
- **Account** (icona in alto a destra): accedi con Google/YouTube e premi "Salva login".
- **↻** (icona in alto): aggiorna yt-dlp se un download fallisce con "Please sign in" / 400.

## Build locale

Requisiti: JDK 17 e Android SDK (API 34).

```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

## Struttura

```
app/src/main/java/com/jdado/ytaudio/
├── YtAudioApp.kt        # init yt-dlp/ffmpeg + auto-update
├── AudioDownloader.kt   # download → MP3, cookies, MediaStore
├── YtSearch.kt          # ricerca YouTube via yt-dlp (JSON)
├── CookieStore.kt       # cookie WebView → cookies.txt (Netscape)
├── LoginScreen.kt       # WebView di login Google/YouTube
├── MainViewModel.kt     # stato: ricerca, download, login
├── MainActivity.kt      # UI Compose (schede Cerca/Link)
└── ui/theme/Theme.kt    # tema Material 3
```

## Note tecniche

- `minSdk 24`, `targetSdk 34`.
- **Login Google in WebView**: Google a volte rifiuta l'accesso in WebView ("browser non sicuro"). È un limite noto lato Google; quando l'accesso va a buon fine i cookie vengono salvati e usati. La ricerca e i download pubblici funzionano comunque senza login.
- Il primo avvio estrae il runtime Python/yt-dlp e ne scarica l'ultima versione: attendi qualche secondo prima del primo download.
- yt-dlp cambia spesso a causa delle modifiche lato YouTube: usa il pulsante ↻ per aggiornarlo, o aggiorna la dipendenza `youtubedl-android`.
