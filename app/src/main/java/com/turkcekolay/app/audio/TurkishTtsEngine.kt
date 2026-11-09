package com.turkcekolay.app.audio

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.turkcekolay.app.domain.model.TURKISH
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

enum class TtsStatus {
    INITIALIZING,
    READY,
    /** Turkish is supported by the engine but its voice data is not downloaded. */
    MISSING_DATA,
    /** The installed engine does not support Turkish at all. */
    NOT_SUPPORTED,
    /** No TTS engine or it failed to start. */
    ERROR,
}

/**
 * Android TextToSpeech configured for tr-TR. A fallback only: authentic native recordings
 * are always preferred (see [AudioRepository]).
 */
class TurkishTtsEngine(context: Context) {

    private val appContext = context.applicationContext
    private val _status = MutableStateFlow(TtsStatus.INITIALIZING)
    val status: StateFlow<TtsStatus> = _status.asStateFlow()

    private var tts: TextToSpeech? = null
    private val waiting = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val counter = AtomicInteger()

    /** True when at least one installed (offline) Turkish voice exists. */
    var hasOfflineTurkishVoice: Boolean = false
        private set

    init {
        start()
    }

    private fun start() {
        _status.value = TtsStatus.INITIALIZING
        tts = runCatching {
            TextToSpeech(appContext) { result ->
                if (result == TextToSpeech.SUCCESS) configure() else _status.value = TtsStatus.ERROR
            }
        }.getOrElse {
            _status.value = TtsStatus.ERROR
            null
        }
    }

    private fun configure() {
        val engine = tts ?: return
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { utteranceId?.let { waiting.remove(it)?.complete(Unit) } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { utteranceId?.let { waiting.remove(it)?.complete(Unit) } }
            override fun onError(utteranceId: String?, errorCode: Int) { utteranceId?.let { waiting.remove(it)?.complete(Unit) } }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { utteranceId?.let { waiting.remove(it)?.complete(Unit) } }
        })
        val result = runCatching { engine.setLanguage(TURKISH) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        _status.value = when (result) {
            TextToSpeech.LANG_MISSING_DATA -> TtsStatus.MISSING_DATA
            TextToSpeech.LANG_NOT_SUPPORTED -> TtsStatus.NOT_SUPPORTED
            else -> TtsStatus.READY
        }
        hasOfflineTurkishVoice = runCatching {
            engine.voices.orEmpty().any { v ->
                v.locale.language == TURKISH.language && !v.isNetworkConnectionRequired &&
                    !v.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }
        }.getOrDefault(false)
        runCatching {
            engine.voices.orEmpty()
                .filter { it.locale.language == TURKISH.language && !it.isNetworkConnectionRequired }
                .maxByOrNull { it.quality }
                ?.let { engine.voice = it }
        }
    }

    /** Speaks [text] at [rate] and suspends until it finishes. Returns false if it could not speak. */
    suspend fun speakAndAwait(text: String, rate: Float): Boolean {
        val engine = tts ?: return false
        if (_status.value != TtsStatus.READY) return false
        val id = "tk_${counter.incrementAndGet()}"
        val done = CompletableDeferred<Unit>()
        waiting[id] = done
        engine.setSpeechRate(rate)
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) {
            waiting.remove(id)
            return false
        }
        withTimeoutOrNull(15_000L) { done.await() }
        waiting.remove(id)
        return true
    }

    fun stop() {
        tts?.stop()
        waiting.values.forEach { it.complete(Unit) }
        waiting.clear()
    }

    /** Re-checks after the learner returns from installing voice data. */
    fun refresh() {
        tts?.shutdown()
        start()
    }

    /** Intent that opens the system screen for downloading TTS voice data. */
    fun installVoiceIntent(): Intent =
        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
