package com.turkcekolay.app.ai

import com.turkcekolay.app.domain.model.LearningState
import com.turkcekolay.app.domain.repository.ContentRepository
import com.turkcekolay.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiTutorRepository @Inject constructor(
    private val provider: AiProvider,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
) {
    val isAvailable: Boolean get() = provider.isConfigured

    suspend fun ask(history: List<TutorWireMessage>, mode: TutorMode): Result<String> {
        val known = progress.observeItemProgress().first()
            .values.filter { it.state != LearningState.NEW }
            .map { it.vocabularyId }
        val words = content.getItems(known.take(60)).map { it.turkish }
        val level = when {
            known.size >= 120 -> "A2"
            known.size >= 40 -> "A1"
            else -> "L0"
        }
        return provider.reply(
            TutorRequest(messages = history, mode = mode.name, learnerLevel = level, knownWords = words),
        )
    }
}
