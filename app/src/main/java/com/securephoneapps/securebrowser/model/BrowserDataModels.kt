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
    val isSuspendedState: Boolean = false // True if serialized and suspended
)

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
