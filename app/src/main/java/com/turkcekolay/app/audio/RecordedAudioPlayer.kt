package com.turkcekolay.app.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plays native-speaker recordings with Media3. Slow mode uses Media3's pitch-preserving
 * time stretching at a moderate 0.75× so pronunciation is not distorted.
 * Must be used from the main thread.
 */
class RecordedAudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private fun obtain(): ExoPlayer = player ?: ExoPlayer.Builder(appContext).build().also { p ->
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
        player = p
    }

    fun play(uri: Uri, slow: Boolean) {
        val p = obtain()
        p.stop()
        p.setMediaItem(MediaItem.fromUri(uri))
        p.playbackParameters = PlaybackParameters(if (slow) SLOW_SPEED else 1f)
        p.prepare()
        p.play()
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    fun stop() {
        player?.stop()
    }

    fun release() {
        player?.release()
        player = null
    }

    companion object {
        const val SLOW_SPEED = 0.75f
    }
}
