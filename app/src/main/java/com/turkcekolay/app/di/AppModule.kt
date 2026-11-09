package com.turkcekolay.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.turkcekolay.app.BuildConfig
import com.turkcekolay.app.ai.AiProvider
import com.turkcekolay.app.ai.BackendAiProvider
import com.turkcekolay.app.ai.NotConfiguredAiProvider
import com.turkcekolay.app.audio.AudioManager
import com.turkcekolay.app.audio.AudioRepository
import com.turkcekolay.app.audio.RecordedAudioPlayer
import com.turkcekolay.app.audio.TurkishTtsEngine
import com.turkcekolay.app.core.common.Clock
import com.turkcekolay.app.core.common.SystemClock
import com.turkcekolay.app.data.local.dao.ContentDao
import com.turkcekolay.app.data.local.dao.ProgressDao
import com.turkcekolay.app.data.local.database.AppDatabase
import com.turkcekolay.app.data.preferences.SettingsRepositoryImpl
import com.turkcekolay.app.data.preferences.userSettingsDataStore
import com.turkcekolay.app.data.repository.ContentRepositoryImpl
import com.turkcekolay.app.data.repository.ProgressRepositoryImpl
import com.turkcekolay.app.domain.learning.SpacedRepetitionScheduler
import com.turkcekolay.app.domain.repository.ContentRepository
import com.turkcekolay.app.domain.repository.ProgressRepository
import com.turkcekolay.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides fun contentDao(db: AppDatabase): ContentDao = db.contentDao()
    @Provides fun progressDao(db: AppDatabase): ProgressDao = db.progressDao()

    @Provides @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.userSettingsDataStore

    @Provides @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides @Singleton fun clock(): Clock = SystemClock()
    @Provides @Singleton fun scheduler(): SpacedRepetitionScheduler = SpacedRepetitionScheduler()

    @Provides @Singleton
    fun audioManager(@ApplicationContext context: Context, settings: SettingsRepository): AudioManager =
        AudioManager(AudioRepository(context), RecordedAudioPlayer(context), TurkishTtsEngine(context), settings.settings)

    @Provides @Singleton
    fun aiProvider(json: Json): AiProvider {
        val url = BuildConfig.TUTOR_BACKEND_URL
        if (url.isBlank()) return NotConfiguredAiProvider()
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
        return BackendAiProvider(url, client, json)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun content(impl: ContentRepositoryImpl): ContentRepository
    @Binds @Singleton abstract fun progress(impl: ProgressRepositoryImpl): ProgressRepository
    @Binds @Singleton abstract fun settings(impl: SettingsRepositoryImpl): SettingsRepository
}
