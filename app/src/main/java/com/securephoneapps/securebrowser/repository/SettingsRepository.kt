package com.securephoneapps.securebrowser.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsRepository(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_browser_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default
    fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    
    fun setString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun setBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    
    companion object {
        const val KEY_APP_THEME = "app_theme"
        const val KEY_THEME_COLOR = "theme_color"
        const val KEY_SEARCH_ENGINE = "search_engine"
        const val KEY_CUSTOM_SEARCH_URL = "custom_search_url"
        const val KEY_AD_BLOCK_ENABLED = "ad_block_enabled"
        const val KEY_HTTPS_ONLY = "https_only"
        const val KEY_FORCE_DARK_MODE = "forced_dark_mode_enabled"
        const val KEY_JS_ENABLED = "js_enabled"
        const val KEY_WEB_RTC_PRIVACY = "web_rtc_privacy"
        const val KEY_DE_AMP = "de_amp_enabled"
        const val KEY_FINGERPRINT_PROTECTION = "fingerprint_protection_enabled"
        const val KEY_STRICT_LOCAL_RESTRICTION = "restrict_local_subnets"
        const val KEY_CUSTOM_USER_AGENT = "custom_user_agent"
    }
}
