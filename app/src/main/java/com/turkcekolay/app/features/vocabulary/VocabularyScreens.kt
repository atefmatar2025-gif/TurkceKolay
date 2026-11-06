@file:OptIn(ExperimentalMaterial3Api::class)

package com.turkcekolay.app.features.vocabulary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcekolay.app.core.designsystem.CategoryCard
import com.turkcekolay.app.core.designsystem.EmptyState
import com.turkcekolay.app.core.designsystem.LoadingState
import com.turkcekolay.app.core.designsystem.PrimaryButton
import com.turkcekolay.app.core.designsystem.SectionTitle
import com.turkcekolay.app.core.designsystem.TkTopBar
import com.turkcekolay.app.core.designsystem.VocabularyCard
import com.turkcekolay.app.domain.model.CategoryGroup
import com.turkcekolay.app.domain.model.PresentationMode
import com.turkcekolay.app.features.exercises.PracticeContent
import com.turkcekolay.app.features.lessons.ResultContent

@Composable
fun CategoriesScreen(onBack: () -> Unit, onOpenCategory: (String) -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TkTopBar("المفردات المصوّرة", onBack)
        if (categories == null) { LoadingState(); return }
        val groups = categories.orEmpty().groupBy { it.group }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(
                CategoryGroup.FIRST_WORDS to "للبداية",
                CategoryGroup.EXPRESSIONS to "عبارات",
                CategoryGroup.EVERYDAY to "الحياة اليومية",
            ).forEach { (group, title) ->
                val list = groups[group].orEmpty()
                if (list.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(title) }
                    items(list, key = { it.id }) { c ->
                        CategoryCard(c.titleArabic, c.titleTurkish, c.image, { onOpenCategory(c.id) })
                    }
                }
            }
        }
    }
}

/** Word cards for a category (or the favorites list) with presentation modes A–E and practice. */
@Composable
fun WordsScreen(onBack: () -> Unit, viewModel: WordsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val practice by viewModel.practice.collectAsStateWithLifecycle()

    when (val mode = state.mode) {
        WordsMode.Browse -> Unit
        WordsMode.Practice -> {
            BackHandler { viewModel.stopPractice() }
            val ui = practice
            val exercise = ui?.exercise
            if (ui != null && exercise != null) {
                PracticeContent(
                    exercise, ui.progress, state.quietMode, state.slowAudio,
                    onAnswered = { r, s -> viewModel.answer(r, s) },
                    onContinue = viewModel::next,
                    onClose = viewModel::stopPractice,
                )
            } else LoadingState()
            return
        }
        is WordsMode.Result -> {
            ResultContent("أحسنت التدريب! 🎉", mode.score, mode.correct, mode.total, mode.xp, onDone = viewModel::stopPractice)
            return
        }
    }

    Column(Modifier.fillMaxSize()) {
        TkTopBar(state.title, onBack)
        if (state.loading) { LoadingState(); return }
        if (state.items.isEmpty()) {
            EmptyState(
                if (state.isFavorites) "لا توجد مفضلة بعد" else "لا توجد كلمات",
                if (state.isFavorites) "اضغط ♡ على أي بطاقة لتجدها هنا." else "سنضيف كلمات قريبًا.",
            )
            return
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(PresentationMode.entries.toList()) { m ->
                val disabled = state.quietMode && (m == PresentationMode.D || m == PresentationMode.E)
                FilterChip(
                    selected = state.selectedMode == m,
                    onClick = { viewModel.selectMode(m) },
                    enabled = !disabled,
                    label = { Text("${m.label}: ${m.description}") },
                )
            }
        }
        val pager = rememberPagerState(pageCount = { state.items.size })
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            pageSpacing = 12.dp,
        ) { page ->
            val item = state.items[page]
            val m = state.selectedMode ?: state.defaultModes[item.id] ?: PresentationMode.A
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                VocabularyCard(
                    image = item.image,
                    turkish = item.turkish,
                    arabic = item.arabic,
                    audio = item.audio,
                    showTurkish = m == PresentationMode.A || m == PresentationMode.B,
                    showArabic = m == PresentationMode.A,
                    showImage = m != PresentationMode.E,
                    example = item.exampleTurkish?.let { it to item.exampleArabic.orEmpty() },
                    exampleAudio = item.exampleAudio,
                    note = item.note,
                    isFavorite = item.id in state.favorites,
                    onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                    autoPlayKey = if (!state.quietMode && pager.currentPage == page && (m == PresentationMode.D || m == PresentationMode.E)) "${item.id}$m" else null,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${pager.currentPage + 1} / ${state.items.size}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (state.items.size >= 3) PrimaryButton("تدرّب الآن", viewModel::startPractice)
        }
    }
}
