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
    fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    
    fun setString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun setBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun setInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    
    companion object {
        const val KEY_APP_THEME = "app_theme"
        const val KEY_THEME_COLOR = "theme_color"
        const val KEY_SEARCH_ENGINE = "search_engine"
        const val KEY_CUSTOM_SEARCH_URL = "custom_search_url"
        const val KEY_AD_BLOCK_ENABLED = "ad_block_enabled"
        const val KEY_HTTPS_ONLY = "https_only_mode"
        const val KEY_FORCE_DARK_MODE = "forced_dark_mode_enabled"
        const val KEY_JS_ENABLED = "javascript_enabled"
        const val KEY_WEB_RTC_PRIVACY = "webrtc_privacy_enabled"
        const val KEY_DE_AMP = "deamp_enabled"
        const val KEY_FINGERPRINT_PROTECTION = "fingerprint_protection_enabled"
        const val KEY_STRICT_LOCAL_RESTRICTION = "restrict_local_subnets"
        const val KEY_CUSTOM_USER_AGENT = "custom_user_agent"
        const val KEY_BLOCK_THIRD_PARTY_COOKIES = "block_third_party_cookies"
        const val KEY_DE_GOOGLING = "de_googling_telemetry_enabled"
        const val KEY_CLEAR_ON_EXIT = "clear_on_exit_enabled"
        const val KEY_STRIP_TRACKING = "strip_tracking_enabled"
        const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"
        const val KEY_HARDWARE_SHUTTER = "hardware_shutter_active"
        const val KEY_ADDRESS_BAR_POSITION = "address_bar_position"
    }
}
