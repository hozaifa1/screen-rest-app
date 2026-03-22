package com.screenrest.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.messageIndexDataStore by preferencesDataStore("message_index")

@Singleton
class MessageIndexDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val AYAH_INDEX_KEY = intPreferencesKey("ayah_index")
    private val REMINDER_INDEX_KEY = intPreferencesKey("reminder_index")
    private val LAST_MESSAGE_TYPE_KEY = intPreferencesKey("last_message_type")
    private val THEME_INDEX_KEY = intPreferencesKey("theme_index")

    val ayahIndex: Flow<Int> = context.messageIndexDataStore.data.map { preferences ->
        preferences[AYAH_INDEX_KEY] ?: 0
    }

    val reminderIndex: Flow<Int> = context.messageIndexDataStore.data.map { preferences ->
        preferences[REMINDER_INDEX_KEY] ?: 0
    }

    val lastMessageType: Flow<Int> = context.messageIndexDataStore.data.map { preferences ->
        preferences[LAST_MESSAGE_TYPE_KEY] ?: 0 // 0 = Reminder, 1 = Ayah
    }

    val themeIndex: Flow<Int> = context.messageIndexDataStore.data.map { preferences ->
        preferences[THEME_INDEX_KEY] ?: 0
    }

    suspend fun incrementAyahIndex(newIndex: Int) {
        context.messageIndexDataStore.edit { preferences ->
            preferences[AYAH_INDEX_KEY] = newIndex
        }
    }

    suspend fun incrementReminderIndex(newIndex: Int) {
        context.messageIndexDataStore.edit { preferences ->
            preferences[REMINDER_INDEX_KEY] = newIndex
        }
    }

    suspend fun setLastMessageType(type: Int) {
        context.messageIndexDataStore.edit { preferences ->
            preferences[LAST_MESSAGE_TYPE_KEY] = type
        }
    }

    suspend fun incrementThemeIndex(newIndex: Int) {
        context.messageIndexDataStore.edit { preferences ->
            preferences[THEME_INDEX_KEY] = newIndex
        }
    }
}
