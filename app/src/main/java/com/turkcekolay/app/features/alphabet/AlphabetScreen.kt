package com.turkcekolay.app.features.alphabet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.turkcekolay.app.core.designsystem.LoadingState
import com.turkcekolay.app.core.designsystem.PrimaryButton
import com.turkcekolay.app.core.designsystem.TkCard
import com.turkcekolay.app.core.designsystem.TkColors
import com.turkcekolay.app.core.designsystem.TkTopBar
import com.turkcekolay.app.core.designsystem.TurkishStyle
import com.turkcekolay.app.core.designsystem.TurkishText
import com.turkcekolay.app.data.content.ContentSeeder
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.domain.model.LearningState
import com.turkcekolay.app.domain.repository.ContentRepository
import com.turkcekolay.app.domain.repository.ProgressRepository
import com.turkcekolay.app.domain.repository.SettingsRepository
import com.turkcekolay.app.features.lessons.LearnCardsContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlphabetUi(
    val loading: Boolean = true,
    val letters: List<LearnItem> = emptyList(),
    val learned: Set<String> = emptySet(),
    val favorites: Set<String> = emptySet(),
    val quietMode: Boolean = false,
)

@HiltViewModel
class AlphabetViewModel @Inject constructor(
    content: ContentRepository,
    private val progress: ProgressRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<AlphabetUi> = combine(
        content.observeItemsByCategory(ContentSeeder.ALPHABET_CATEGORY),
        progress.observeItemProgress(),
        progress.observeFavorites(),
        settings.settings,
    ) { letters, prog, favs, s ->
        AlphabetUi(
            loading = false,
            letters = letters,
            learned = prog.filterValues { it.state != LearningState.NEW && it.correctCount > 0 }.keys,
            favorites = favs,
            quietMode = s.quietMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlphabetUi())

    fun toggleFavorite(id: String) = viewModelScope.launch { progress.toggleFavorite(id) }
}

private val SPECIAL = setOf("Ç", "Ş", "Ğ", "I", "İ", "Ö", "Ü")

@Composable
fun AlphabetScreen(onBack: () -> Unit, onStartLesson: (String) -> Unit, viewModel: AlphabetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf<Int?>(null) }

    if (state.loading) { LoadingState(); return }

    val index = open
    if (index != null && state.letters.isNotEmpty()) {
        BackHandler { open = null }
        LearnCardsContent(
            title = "الحروف",
            items = state.letters,
            index = index.coerceIn(0, state.letters.lastIndex),
            favorites = state.favorites,
            quietMode = state.quietMode,
            onToggleFavorite = viewModel::toggleFavorite,
            onPrevious = { open = (index - 1).coerceAtLeast(0) },
            onNext = { open = if (index < state.letters.lastIndex) index + 1 else null },
            onClose = { open = null },
            lastLabel = "انتهيت",
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        TkTopBar("الحروف التركية (29)", onBack)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(76.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { DotlessExplainer() }
            itemsIndexed(state.letters, key = { _, l -> l.id }) { i, letter ->
                val special = letter.turkish in SPECIAL
                val learned = letter.id in state.learned
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (learned) TkColors.SuccessSoft else MaterialTheme.colorScheme.surface)
                        .border(2.dp, if (special) TkColors.Orange else MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                        .clickable(role = Role.Button) { open = i },
                    contentAlignment = Alignment.Center,
                ) {
                    TurkishText("${letter.turkish}${letter.turkishLower}", style = TurkishStyle.Large, color = MaterialTheme.colorScheme.secondary)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 8.dp)) {
                    Text("🟧 الإطار البرتقالي = حروف خاصة بالتركية", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryButton("ابدأ درس الحروف", { onStartLesson("alpha_1") }, Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun DotlessExplainer() {
    TkCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Column(Modifier.padding(16.dp)) {
            Text("انتبه: حرفان مختلفان!", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TurkishText("I ı", style = TurkishStyle.Large)
                    Text("بلا نقطة", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TurkishText("İ i", style = TurkishStyle.Large)
                    Text("بنقطة", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                "الحرف الكبير من i هو İ (بنقطة)، والحرف الكبير من ı هو I (بلا نقطة). النقطة تغيّر الصوت والمعنى.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
