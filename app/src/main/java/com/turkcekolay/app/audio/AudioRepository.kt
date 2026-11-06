package com.turkcekolay.app.audio

import android.content.Context
import android.net.Uri
import com.turkcekolay.app.domain.model.AudioRef
import java.io.File

sealed interface PlaybackSource {
    data class Recording(val uri: Uri, val origin: Origin) : PlaybackSource
    data class Speech(val text: String) : PlaybackSource
    data object Unavailable : PlaybackSource

    enum class Origin { DOWNLOADED, CACHED, BUNDLED }
}

/**
 * Resolves where a piece of audio can come from, in priority order:
 * 1. Downloaded native recording  (filesDir/audio/<folder>/<file>)
 * 2. Cached native recording      (cacheDir/audio/<folder>/<file>)
 * 3. Bundled native recording     (assets/audio/<folder>/<file>)
 * 4. Android Turkish TTS
 * 5. Unavailable — the UI then shows a disabled audio button with an explanation.
 */
class AudioRepository(context: Context) {

    private val appContext = context.applicationContext
    private val bundledIndex = mutableMapOf<String, Set<String>>()

    fun resolve(ref: AudioRef, ttsReady: Boolean): PlaybackSource {
        val relative = "audio/${ref.folder}/${ref.assetFileName}"
        File(appContext.filesDir, relative).takeIf { it.isFile && it.length() > 0 }?.let {
            return PlaybackSource.Recording(Uri.fromFile(it), PlaybackSource.Origin.DOWNLOADED)
        }
        File(appContext.cacheDir, relative).takeIf { it.isFile && it.length() > 0 }?.let {
            return PlaybackSource.Recording(Uri.fromFile(it), PlaybackSource.Origin.CACHED)
        }
        if (ref.assetFileName in bundledFiles(ref.folder)) {
            return PlaybackSource.Recording(Uri.parse("asset:///$relative"), PlaybackSource.Origin.BUNDLED)
        }
        if (ttsReady && ref.text.isNotBlank()) return PlaybackSource.Speech(ref.text)
        return PlaybackSource.Unavailable
    }

    fun hasRecording(ref: AudioRef): Boolean = resolve(ref, ttsReady = false) is PlaybackSource.Recording

    @Synchronized
    private fun bundledFiles(folder: String): Set<String> = bundledIndex.getOrPut(folder) {
        runCatching { appContext.assets.list("audio/$folder")?.toSet() }.getOrNull().orEmpty()
    }
}
