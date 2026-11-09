package com.turkcekolay.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.turkcekolay.app.core.common.Clock
import com.turkcekolay.app.data.local.dao.ProgressDao
import com.turkcekolay.app.data.local.entities.UserProfileEntity
import com.turkcekolay.app.domain.model.SpeechSpeed
import com.turkcekolay.app.domain.model.UserSettings
import com.turkcekolay.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * DataStore is the source of truth for preferences. The Room UserProfile row mirrors the
 * profile-relevant fields so a future cloud sync can upload a single profile record.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val progressDao: ProgressDao,
    private val clock: Clock,
) : SettingsRepository {

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val GOAL = intPreferencesKey("daily_goal_minutes")
        val QUIET = booleanPreferencesKey("quiet_mode")
        val SPEED = stringPreferencesKey("speech_speed")
        val REPEAT = intPreferencesKey("repeat_count")
        val REMINDERS = booleanPreferencesKey("reminders")
        val UNLOCK_ALL = booleanPreferencesKey("unlock_all_levels")
        val CONTENT_VERSION = intPreferencesKey("content_version")
    }

    override val settings: Flow<UserSettings> = dataStore.data.map { p ->
        UserSettings(
            onboardingCompleted = p[Keys.ONBOARDING] ?: false,
            dailyGoalMinutes = p[Keys.GOAL] ?: 10,
            quietMode = p[Keys.QUIET] ?: false,
            speechSpeed = p[Keys.SPEED]?.let { v -> SpeechSpeed.entries.firstOrNull { it.name == v } } ?: SpeechSpeed.SLOW,
            repeatCount = (p[Keys.REPEAT] ?: 3).coerceIn(1, 3),
            remindersEnabled = p[Keys.REMINDERS] ?: false,
            unlockAllLevels = p[Keys.UNLOCK_ALL] ?: false,
        )
    }

    override suspend fun completeOnboarding(dailyGoalMinutes: Int, quietMode: Boolean) {
        dataStore.edit {
            it[Keys.ONBOARDING] = true
            it[Keys.GOAL] = dailyGoalMinutes
            it[Keys.QUIET] = quietMode
        }
        syncProfile()
    }

    override suspend fun setDailyGoal(minutes: Int) {
        dataStore.edit { it[Keys.GOAL] = minutes }
        syncProfile()
    }

    override suspend fun setQuietMode(enabled: Boolean) {
        dataStore.edit { it[Keys.QUIET] = enabled }
        syncProfile()
    }

    override suspend fun setSpeechSpeed(speed: SpeechSpeed) { dataStore.edit { it[Keys.SPEED] = speed.name } }
    override suspend fun setRepeatCount(count: Int) { dataStore.edit { it[Keys.REPEAT] = count.coerceIn(1, 3) } }
    override suspend fun setReminders(enabled: Boolean) { dataStore.edit { it[Keys.REMINDERS] = enabled } }
    override suspend fun setUnlockAll(enabled: Boolean) { dataStore.edit { it[Keys.UNLOCK_ALL] = enabled } }

    override suspend fun contentVersion(): Int = dataStore.data.first()[Keys.CONTENT_VERSION] ?: 0
    override suspend fun setContentVersion(version: Int) { dataStore.edit { it[Keys.CONTENT_VERSION] = version } }

    private suspend fun syncProfile() {
        val s = settings.first()
        val now = clock.now()
        val existing = progressDao.getProfile(UserProfileEntity.DEFAULT_ID)
        progressDao.upsertProfile(
            (existing ?: UserProfileEntity(createdAt = now, updatedAt = now)).copy(
                dailyGoalMinutes = s.dailyGoalMinutes,
                quietModeEnabled = s.quietMode,
                updatedAt = now,
            ),
        )
    }
}
