package com.turkcekolay.app.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.turkcekolay.app.core.designsystem.LoadingState
import com.turkcekolay.app.core.designsystem.SectionTitle
import com.turkcekolay.app.core.designsystem.TkTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CreditEntry(
    val item: String,
    val title: String,
    val author: String = "",
    val license: String = "",
    val page: String = "",
    val speaker: String = "",
)

@Serializable
data class Credits(val images: List<CreditEntry> = emptyList(), val audio: List<CreditEntry> = emptyList())

/** Attribution for every bundled photo and recording (required by their Creative Commons licences). */
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val credits by produceState<Credits?>(null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("credits.json").bufferedReader().use {
                    Json { ignoreUnknownKeys = true }.decodeFromString(Credits.serializer(), it.readText())
                }
            }.getOrDefault(Credits())
        }
    }
    Column(Modifier.fillMaxSize()) {
        TkTopBar("مصادر الصور والأصوات", onBack)
        val c = credits ?: return@Column LoadingState()
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                Text(
                    "الصور من Wikimedia Commons، والتسجيلات بأصوات ناطقين أصليين من مشروع Lingua Libre. " +
                        "كل ملف يبقى تحت رخصته الأصلية.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item { SectionTitle("الصور (${c.images.size})") }
            items(c.images) { CreditRow(it) }
            item { SectionTitle("الأصوات (${c.audio.size})") }
            items(c.audio) { CreditRow(it) }
        }
    }
}

@Composable
private fun CreditRow(e: CreditEntry) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(e.title, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr))
        Text(
            listOf(e.author.ifBlank { e.speaker }, e.license, e.page).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
