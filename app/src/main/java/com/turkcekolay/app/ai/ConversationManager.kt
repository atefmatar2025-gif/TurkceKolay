package com.turkcekolay.app.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TutorRole { LEARNER, TUTOR }

data class TutorMessage(val role: TutorRole, val text: String, val isError: Boolean = false)

/**
 * Keeps the text conversation with the tutor. Only the most recent turns are sent, which keeps
 * requests small. Designed so a voice layer (speech-to-text in, TTS out) can wrap it later.
 */
class ConversationManager(private val maxTurnsSent: Int = 12) {
    private val _messages = MutableStateFlow(
        listOf(
            TutorMessage(
                TutorRole.TUTOR,
                "أهلًا! أنا معلّمك للغة التركية 🌷\nاسألني عن معنى كلمة، أو اكتب جملة تركية وسأصحّحها لك بلطف.",
            ),
        ),
    )
    val messages: StateFlow<List<TutorMessage>> = _messages.asStateFlow()

    fun addLearner(text: String) = _messages.update { it + TutorMessage(TutorRole.LEARNER, text) }
    fun addTutor(text: String) = _messages.update { it + TutorMessage(TutorRole.TUTOR, text) }
    fun addError(text: String) = _messages.update { it + TutorMessage(TutorRole.TUTOR, text, isError = true) }

    fun wireHistory(): List<TutorWireMessage> = _messages.value
        .drop(1) // the local greeting is not part of the model conversation
        .filterNot { it.isError }
        .takeLast(maxTurnsSent)
        .map { TutorWireMessage(if (it.role == TutorRole.LEARNER) "user" else "assistant", it.text) }

    fun clear() = _messages.update { it.take(1) }
}
