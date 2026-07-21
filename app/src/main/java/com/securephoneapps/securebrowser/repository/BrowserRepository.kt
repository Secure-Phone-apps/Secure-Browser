package com.securephoneapps.securebrowser.repository

import com.securephoneapps.securebrowser.data.SecureBrowserDatabase
import com.securephoneapps.securebrowser.model.BookmarkItem
import com.securephoneapps.securebrowser.model.HistoryItem
import com.securephoneapps.securebrowser.model.ShieldTelemetry
import com.securephoneapps.securebrowser.model.TabGroup
import com.securephoneapps.securebrowser.model.TabInstance
import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val database: SecureBrowserDatabase) {
    val allHistory: Flow<List<HistoryItem>> = database.historyDao().getAllHistory()
    val allBookmarks: Flow<List<BookmarkItem>> = database.bookmarkDao().getAllBookmarks()
    val allTabs: Flow<List<TabInstance>> = database.tabInstanceDao().getAllTabs()
    val allGroups: Flow<List<TabGroup>> = database.tabGroupDao().getAllGroups()
    
    fun getTelemetryFlow(): Flow<ShieldTelemetry?> = database.shieldTelemetryDao().getTelemetryFlow()
    suspend fun getTelemetry(): ShieldTelemetry? = database.shieldTelemetryDao().getTelemetry()
    suspend fun insertTelemetry(telemetry: ShieldTelemetry) = database.shieldTelemetryDao().insertTelemetry(telemetry)
    
    suspend fun insertHistory(item: HistoryItem) = database.historyDao().insertHistoryItem(item)
    suspend fun clearHistory() = database.historyDao().clearAllHistory()
    
    suspend fun insertBookmark(item: BookmarkItem) = database.bookmarkDao().insertBookmark(item)
    suspend fun deleteBookmarkById(id: Long) = database.bookmarkDao().deleteBookmarkById(id)
    suspend fun clearAllBookmarks() = database.bookmarkDao().clearAllBookmarks()
    
    suspend fun insertTab(tab: TabInstance) = database.tabInstanceDao().insertTab(tab)
    suspend fun deleteTabById(tabId: String) = database.tabInstanceDao().deleteTabById(tabId)
    suspend fun clearAllTabs() = database.tabInstanceDao().clearAllTabs()
    
    suspend fun insertGroup(group: TabGroup) = database.tabGroupDao().insertGroup(group)
    suspend fun deleteGroupById(groupId: String) = database.tabGroupDao().deleteGroupById(groupId)
    suspend fun clearAllGroups() = database.tabGroupDao().clearAllGroups()
}
