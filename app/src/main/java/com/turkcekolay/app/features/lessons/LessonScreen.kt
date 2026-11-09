package com.turkcekolay.app.features.lessons

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcekolay.app.core.designsystem.EmptyState
import com.turkcekolay.app.core.designsystem.LoadingState
import com.turkcekolay.app.core.designsystem.PrimaryButton
import com.turkcekolay.app.features.alphabet.AlphabetTourContent
import com.turkcekolay.app.features.exercises.PracticeContent

@Composable
fun LessonScreen(onClose: () -> Unit, viewModel: LessonViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val practice by viewModel.practice.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    BackHandler(onBack = onClose)

    when (val phase = state.phase) {
        LessonPhase.Loading -> LoadingState()
        is LessonPhase.Error -> EmptyState("عذرًا", phase.message, action = { PrimaryButton("رجوع", onClose) })
        LessonPhase.Tour -> AlphabetTourContent(
            letters = state.items,
            quietMode = state.settings.quietMode,
            onClose = onClose,
            onFinished = viewModel::finishTour,
        )
        is LessonPhase.Learn -> LearnCardsContent(
            title = "الخطوة 1 من 2: تعرّف · ${state.lesson?.title.orEmpty()}",
            items = state.items,
            index = phase.index,
            favorites = favorites,
            quietMode = state.settings.quietMode,
            onToggleFavorite = viewModel::toggleFavorite,
            onPrevious = viewModel::previousCard,
            onNext = viewModel::nextCard,
            onClose = onClose,
        )
        LessonPhase.Practice -> {
            val ui = practice
            val exercise = ui?.exercise
            if (ui == null || exercise == null) LoadingState()
            else PracticeContent(
                exercise = exercise,
                progress = ui.progress,
                quietMode = state.settings.quietMode,
                header = "الخطوة 2 من 2: تدرّب",
                onAnswered = { results, selected -> viewModel.answer(results, selected) },
                onContinue = viewModel::next,
                onClose = onClose,
            )
        }
        is LessonPhase.Result -> ResultContent(
            title = "أكملت الدرس! 🎉",
            scorePercent = phase.scorePercent,
            correct = phase.correct,
            total = phase.total,
            xp = phase.xp,
            onDone = onClose,
            onRepeat = viewModel::restart,
        )
    }
}
