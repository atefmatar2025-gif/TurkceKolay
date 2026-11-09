package com.turkcekolay.app.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Plays native-speaker recordings with Media3. Slower speeds use Media3's pitch-preserving
 * time stretching, kept at ≥ 0.7× so pronunciation is not distorted.
 * Must be used from the main thread.
 */
class RecordedAudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private var pending: CompletableDeferred<Unit>? = null

    private fun obtain(): ExoPlayer = player ?: ExoPlayer.Builder(appContext).build().also { p ->
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) pending?.complete(Unit)
            }

            override fun onPlayerError(error: PlaybackException) {
                pending?.complete(Unit)
            }
        })
        player = p
    }

    /** Plays [uri] once and suspends until it ends (or fails). */
    suspend fun playAndAwait(uri: Uri, speed: Float) {
        val p = obtain()
        pending?.complete(Unit)
        val done = CompletableDeferred<Unit>()
        pending = done
        p.stop()
        p.setMediaItem(MediaItem.fromUri(uri))
        p.playbackParameters = PlaybackParameters(speed.coerceIn(MIN_SPEED, 1f))
        p.prepare()
        p.play()
        try {
            withTimeoutOrNull(MAX_CLIP_MS) { done.await() }
        } finally {
            if (pending === done) pending = null
        }
    }

    fun stop() {
        player?.stop()
        pending?.complete(Unit)
    }

    fun release() {
        player?.release()
        player = null
    }

    companion object {
        const val MIN_SPEED = 0.7f
        const val MAX_CLIP_MS = 20_000L
    }
}
