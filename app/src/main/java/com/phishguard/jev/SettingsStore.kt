package com.phishguard.jev

import android.content.Context

/**
 * NOTE: plain SharedPreferences, not encrypted. Fine for a one-day demo on
 * your own device. Before this goes anywhere near a real key you care about
 * long-term, swap this for androidx.security's EncryptedSharedPreferences —
 * it's a five-minute change, same API shape.
 *
 * Also: rotate any key you've ever pasted into a chat window, including the
 * one you tested with earlier.
 */
object SettingsStore {
    private const val PREFS = "jev_phish_guard_prefs"
    private const val KEY_API_KEY = "typesafe_api_key"

    fun getApiKey(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    fun setApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, key)
            .apply()
    }
}
