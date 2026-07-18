# YT Audio → MP3 (Android)

App Android nativa che estrae l'audio da un video di YouTube e lo salva in **MP3 alla massima qualità** (VBR `--audio-quality 0`) nella libreria musicale del dispositivo.

Costruita con **Kotlin + Jetpack Compose** e basata su [`youtubedl-android`](https://github.com/JunkFood02/youtubedl-android) (porting di `yt-dlp`) con **FFmpeg** per la transcodifica.

> ⚠️ **Avviso legale.** Scarica esclusivamente contenuti di cui detieni i diritti, di pubblico dominio o consentiti dalla relativa licenza. L'uso di questa app deve rispettare i Termini di Servizio di YouTube e le leggi sul copyright applicabili. Lo strumento è fornito a solo scopo educativo/uso personale legittimo.

## Funzionalità

- Incolla un link YouTube (o condividilo verso l'app con "Condividi → YT Audio").
- Estrazione della migliore traccia audio disponibile e conversione in MP3.
- Tag ID3 (titolo/artista) e copertina incorporati quando disponibili.
- Barra di avanzamento in tempo reale.
- Salvataggio in `Music/YTAudio/` (visibile nelle app musicali) tramite MediaStore.

## Come ottenere l'APK

Non serve un ambiente locale: a ogni push la **GitHub Action** (`.github/workflows/build.yml`) compila l'APK.

1. Vai nella tab **Actions** del repository.
2. Apri l'ultimo run "Build APK".
3. Scarica l'artifact **`yt-audio-debug-apk`**.
4. Trasferisci l'APK sul telefono e installalo (abilita "Installa da origini sconosciute").

## Build locale

Requisiti: JDK 17 e Android SDK (API 34).

```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

## Struttura

```
app/src/main/java/com/jdado/ytaudio/
├── YtAudioApp.kt        # inizializza yt-dlp + ffmpeg
├── AudioDownloader.kt   # logica di download/transcodifica + MediaStore
├── MainViewModel.kt     # stato UI (Idle/Working/Done/Error)
├── MainActivity.kt      # UI Compose + gestione intent di condivisione
└── ui/theme/Theme.kt    # tema Material 3
```

## Note tecniche

- `minSdk 24`, `targetSdk 34`.
- Su Android 10+ il file viene inserito in `MediaStore.Audio` (nessun permesso di storage necessario). Su Android ≤ 9 usa `WRITE_EXTERNAL_STORAGE`.
- Il primo avvio estrae il runtime Python/yt-dlp: attendi qualche secondo prima del primo download.
- yt-dlp cambia spesso a causa delle modifiche lato YouTube; aggiorna la dipendenza `youtubedl-android` se un download smette di funzionare.
