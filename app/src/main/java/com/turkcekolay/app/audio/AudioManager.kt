package com.turkcekolay.app.audio

import com.turkcekolay.app.domain.model.AudioRef
import kotlinx.coroutines.flow.StateFlow

/** Single entry point for all pronunciation playback in the app. */
class AudioManager(
    private val repository: AudioRepository,
    private val recorded: RecordedAudioPlayer,
    private val tts: TurkishTtsEngine,
) {
    val ttsStatus: StateFlow<TtsStatus> get() = tts.status
    val isPlayingRecording: StateFlow<Boolean> get() = recorded.isPlaying

    private val ttsReady: Boolean get() = tts.status.value == TtsStatus.READY

    /** Whether any playback path exists. Buttons must look disabled when this is false. */
    fun canPlay(ref: AudioRef): Boolean = repository.resolve(ref, ttsReady) !is PlaybackSource.Unavailable

    fun sourceFor(ref: AudioRef): PlaybackSource = repository.resolve(ref, ttsReady)

    /** Plays [ref]. Returns false when nothing could be played. */
    fun play(ref: AudioRef, slow: Boolean = false): Boolean {
        stop()
        return when (val source = repository.resolve(ref, ttsReady)) {
            is PlaybackSource.Recording -> {
                recorded.play(source.uri, slow)
                true
            }
            is PlaybackSource.Speech -> tts.speak(source.text, slow)
            PlaybackSource.Unavailable -> false
        }
    }

    fun pause() = recorded.pause()
    fun resume() = recorded.resume()

    fun stop() {
        recorded.stop()
        tts.stop()
    }

    fun refreshTts() = tts.refresh()
    fun installVoiceIntent() = tts.installVoiceIntent()
}
