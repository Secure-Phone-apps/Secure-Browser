package com.securephoneapps.securebrowser.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_items")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val trackersBlocked: Int = 0
)

@Entity(tableName = "bookmark_items")
data class BookmarkItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val folderPath: String = "", // e.g. "/Root/Security" or "Root"
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tab_instances")
data class TabInstance(
    @PrimaryKey val tabId: String,
    val currentUrl: String,
    val title: String,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val parentGroupId: String? = null,
    val isIncognito: Boolean = false,
    val isSuspendedState: Boolean = false, // True if serialized and suspended
    val serializedEngineState: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TabInstance
        if (tabId != other.tabId) return false
        if (currentUrl != other.currentUrl) return false
        if (title != other.title) return false
        if (lastActiveTimestamp != other.lastActiveTimestamp) return false
        if (parentGroupId != other.parentGroupId) return false
        if (isIncognito != other.isIncognito) return false
        if (isSuspendedState != other.isSuspendedState) return false
        if (serializedEngineState != null) {
            if (other.serializedEngineState == null) return false
            if (!serializedEngineState.contentEquals(other.serializedEngineState)) return false
        } else if (other.serializedEngineState != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = tabId.hashCode()
        result = 31 * result + currentUrl.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + lastActiveTimestamp.hashCode()
        result = 31 * result + (parentGroupId?.hashCode() ?: 0)
        result = 31 * result + isIncognito.hashCode()
        result = 31 * result + isSuspendedState.hashCode()
        result = 31 * result + (serializedEngineState?.contentHashCode() ?: 0)
        return result
    }
}

@Entity(tableName = "tab_groups")
data class TabGroup(
    @PrimaryKey val groupId: String,
    val groupName: String,
    val colorBadgeHex: String // e.g. "#FF3B30", "#34C759"
)

@Entity(tableName = "shield_telemetry")
data class ShieldTelemetry(
    @PrimaryKey val id: Int = 0, // Always 0 to represent singleton state
    val trackersBlockedGlobal: Long = 0,
    val canvasFakesTriggered: Long = 0,
    val fingerprintMocksTriggered: Long = 0,
    val telemetryPingsNeutralized: Long = 0
)
