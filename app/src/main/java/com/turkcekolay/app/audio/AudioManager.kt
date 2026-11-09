package com.turkcekolay.app.audio

import com.turkcekolay.app.domain.model.AudioRef
import com.turkcekolay.app.domain.model.SpeechSpeed
import com.turkcekolay.app.domain.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One step in a spoken sequence: say [ref] [times] times, with pauses in between. */
data class SpeechStep(val ref: AudioRef, val times: Int = 1, val extraSlow: Boolean = false, val pauseAfterMs: Long = 600)

/**
 * Single entry point for all pronunciation playback.
 * - Uses the learner's chosen speed (slow by default, for absolute beginners).
 * - "Repeat" plays a word several times (3 by default) with a short pause, so the ear can catch it.
 * - Plays sequences (letter name → example word) and can be stopped at any time.
 */
class AudioManager(
    private val repository: AudioRepository,
    private val recorded: RecordedAudioPlayer,
    private val tts: TurkishTtsEngine,
    settings: Flow<UserSettings>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    @Volatile private var speed: SpeechSpeed = SpeechSpeed.SLOW
    @Volatile var repeatCount: Int = 3
        private set

    private val _playingId = MutableStateFlow<String?>(null)
    /** Content id currently being spoken, for "now playing" highlights. */
    val playingId: StateFlow<String?> = _playingId.asStateFlow()

    val ttsStatus: StateFlow<TtsStatus> get() = tts.status

    init {
        scope.launch {
            settings.collect {
                speed = it.speechSpeed
                repeatCount = it.repeatCount.coerceIn(1, 3)
            }
        }
    }

    private val ttsReady: Boolean get() = tts.status.value == TtsStatus.READY

    /** Whether any playback path exists. Buttons must look disabled when this is false. */
    fun canPlay(ref: AudioRef): Boolean = repository.resolve(ref, ttsReady) !is PlaybackSource.Unavailable

    fun sourceFor(ref: AudioRef): PlaybackSource = repository.resolve(ref, ttsReady)

    /** Plays [ref]; with [repeat] it is said [repeatCount] times. Returns false when nothing can play. */
    fun play(ref: AudioRef, slow: Boolean = false, repeat: Boolean = false): Boolean {
        if (!canPlay(ref)) return false
        playSequence(listOf(SpeechStep(ref, if (repeat) repeatCount else 1, extraSlow = slow)))
        return true
    }

    /** Plays several steps one after another. A new call interrupts the previous one. */
    fun playSequence(steps: List<SpeechStep>, onFinished: () -> Unit = {}): Job {
        stop()
        val newJob = scope.launch {
            try {
                for (step in steps) {
                    repeat(step.times) { i ->
                        _playingId.value = step.ref.contentId
                        speakOnce(step.ref, step.extraSlow)
                        if (i < step.times - 1) delay(BETWEEN_REPEATS_MS)
                    }
                    delay(step.pauseAfterMs)
                }
            } finally {
                _playingId.value = null
            }
            onFinished()
        }
        job = newJob
        return newJob
    }

    private suspend fun speakOnce(ref: AudioRef, extraSlow: Boolean) {
        when (val source = repository.resolve(ref, ttsReady)) {
            is PlaybackSource.Recording ->
                recorded.playAndAwait(source.uri, if (extraSlow) RecordedAudioPlayer.MIN_SPEED else speed.recordingSpeed)
            is PlaybackSource.Speech ->
                tts.speakAndAwait(source.text, if (extraSlow) EXTRA_SLOW_TTS else speed.ttsRate)
            PlaybackSource.Unavailable -> Unit
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        recorded.stop()
        tts.stop()
        _playingId.value = null
    }

    fun refreshTts() = tts.refresh()
    fun installVoiceIntent() = tts.installVoiceIntent()

    companion object {
        const val BETWEEN_REPEATS_MS = 900L
        const val EXTRA_SLOW_TTS = 0.45f
    }
}
