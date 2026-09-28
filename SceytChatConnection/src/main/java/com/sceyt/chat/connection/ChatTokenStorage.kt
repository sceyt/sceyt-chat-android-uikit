package com.sceyt.chat.connection

import android.content.Context
import androidx.core.content.edit

internal interface ChatTokenStorage {
    fun get(userId: String): String?

    fun save(userId: String, token: String)

    fun clear()
}

internal class SharedPreferencesChatTokenStorage(context: Context) : ChatTokenStorage {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override fun get(userId: String): String? =
        if (preferences.getString(USER_ID_KEY, null) == userId) {
            preferences.getString(TOKEN_KEY, null)
        } else {
            null
        }

    override fun save(userId: String, token: String) {
        preferences.edit {
            putString(USER_ID_KEY, userId)
            putString(TOKEN_KEY, token)
        }
    }

    override fun clear() {
        preferences.edit { clear() }
    }

    private companion object {
        const val PREFERENCES_NAME = "sceyt_chat_connection"
        const val USER_ID_KEY = "user_id"
        const val TOKEN_KEY = "token"
    }
}
