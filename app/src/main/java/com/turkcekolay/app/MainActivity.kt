package com.turkcekolay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.turkcekolay.app.audio.AudioManager
import com.turkcekolay.app.core.designsystem.EmptyState
import com.turkcekolay.app.core.designsystem.LoadingState
import com.turkcekolay.app.core.designsystem.LocalAudioManager
import com.turkcekolay.app.core.designsystem.PrimaryButton
import com.turkcekolay.app.core.designsystem.TurkceKolayTheme
import com.turkcekolay.app.core.navigation.AppNavHost
import com.turkcekolay.app.domain.repository.ContentRepository
import com.turkcekolay.app.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StartupState {
    data object Loading : StartupState
    data class Ready(val onboardingCompleted: Boolean) : StartupState
    data object Failed : StartupState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val content: ContentRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init { start() }

    fun start() = viewModelScope.launch {
        _state.value = StartupState.Loading
        _state.value = runCatching {
            content.ensureContentReady()
            StartupState.Ready(settings.settings.first().onboardingCompleted)
        }.getOrElse { StartupState.Failed }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var audioManager: AudioManager

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TurkceKolayTheme {
                CompositionLocalProvider(LocalAudioManager provides audioManager) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        when (val s = state) {
                            StartupState.Loading -> LoadingState("نجهّز دروسك…")
                            StartupState.Failed -> EmptyState(
                                "حدث خطأ أثناء تجهيز الدروس",
                                "حاول مرة أخرى. إن تكرّر الخطأ أعد تثبيت التطبيق.",
                                action = { PrimaryButton("إعادة المحاولة", { viewModel.start() }) },
                            )
                            is StartupState.Ready -> AppNavHost(onboardingCompleted = s.onboardingCompleted)
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        audioManager.stop()
    }
}
