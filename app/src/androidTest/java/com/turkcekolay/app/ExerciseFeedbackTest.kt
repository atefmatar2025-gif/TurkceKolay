package com.turkcekolay.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.turkcekolay.app.core.designsystem.ExerciseFeedback
import com.turkcekolay.app.core.designsystem.TurkceKolayTheme
import com.turkcekolay.app.domain.model.ChoiceExercise
import com.turkcekolay.app.domain.model.ChoiceOption
import com.turkcekolay.app.domain.model.ExerciseType
import com.turkcekolay.app.features.exercises.ExerciseHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseFeedbackTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun wrongAnswerShowsKindExplanation() {
        rule.setContent {
            TurkceKolayTheme {
                ExerciseFeedback(visible = true, correct = false, explanation = "«Elma» تعني «تفاحة».", onContinue = {})
            }
        }
        rule.onNodeWithText("«Elma» تعني «تفاحة».").assertIsDisplayed()
        rule.onNodeWithText("متابعة").assertIsDisplayed()
    }

    @Test
    fun choiceExerciseReportsFirstAnswer() {
        var reported: Map<String, Boolean>? = null
        var continued = false
        val exercise = ChoiceExercise(
            id = "t1",
            type = ExerciseType.WORD_SELECTION,
            instruction = "ما اسم هذه الصورة بالتركية؟",
            targetItemId = "w_elma",
            promptImage = "emoji:🍎",
            options = listOf(ChoiceOption("Su", isTurkish = true), ChoiceOption("Elma", isTurkish = true), ChoiceOption("Ev", isTurkish = true)),
            correctIndex = 1,
            explanation = "«Elma» تعني «تفاحة».",
        )
        rule.setContent {
            TurkceKolayTheme {
                ExerciseHost(exercise, quietMode = true, onAnswered = { r, _ -> reported = r }, onContinue = { continued = true })
            }
        }
        rule.onNodeWithText("Elma").performClick()
        rule.onNodeWithText("متابعة").performClick()
        assertEquals(mapOf("w_elma" to true), reported)
        assertTrue(continued)
    }
}
