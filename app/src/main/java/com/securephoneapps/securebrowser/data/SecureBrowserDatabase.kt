package com.securephoneapps.securebrowser.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.securephoneapps.securebrowser.model.BookmarkItem
import com.securephoneapps.securebrowser.model.HistoryItem
import com.securephoneapps.securebrowser.model.ShieldTelemetry
import com.securephoneapps.securebrowser.model.TabGroup
import com.securephoneapps.securebrowser.model.TabInstance
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_items ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryItem)

    @Delete
    suspend fun deleteHistoryItem(item: HistoryItem)

    @Query("DELETE FROM history_items")
    suspend fun clearAllHistory()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmark_items ORDER BY createdTimestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(item: BookmarkItem)

    @Delete
    suspend fun deleteBookmark(item: BookmarkItem)

    @Query("DELETE FROM bookmark_items WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)
}

@Dao
interface TabInstanceDao {
    @Query("SELECT * FROM tab_instances ORDER BY lastActiveTimestamp DESC")
    fun getAllTabs(): Flow<List<TabInstance>>

    @Query("SELECT * FROM tab_instances WHERE tabId = :tabId LIMIT 1")
    suspend fun getTabById(tabId: String): TabInstance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabInstance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTabs(tabs: List<TabInstance>)

    @Delete
    suspend fun deleteTab(tab: TabInstance)

    @Query("DELETE FROM tab_instances WHERE tabId = :tabId")
    suspend fun deleteTabById(tabId: String)

    @Query("DELETE FROM tab_instances")
    suspend fun clearAllTabs()
}

@Dao
interface TabGroupDao {
    @Query("SELECT * FROM tab_groups")
    fun getAllGroups(): Flow<List<TabGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: TabGroup)

    @Delete
    suspend fun deleteGroup(group: TabGroup)

    @Query("DELETE FROM tab_groups WHERE groupId = :groupId")
    suspend fun deleteGroupById(groupId: String)

    @Query("DELETE FROM tab_groups")
    suspend fun clearAllGroups()
}

@Dao
interface ShieldTelemetryDao {
    @Query("SELECT * FROM shield_telemetry WHERE id = 0 LIMIT 1")
    fun getTelemetryFlow(): Flow<ShieldTelemetry?>

    @Query("SELECT * FROM shield_telemetry WHERE id = 0 LIMIT 1")
    suspend fun getTelemetry(): ShieldTelemetry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetry(telemetry: ShieldTelemetry)
}

@Database(
    entities = [
        HistoryItem::class,
        BookmarkItem::class,
        TabInstance::class,
        TabGroup::class,
        ShieldTelemetry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SecureBrowserDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tabInstanceDao(): TabInstanceDao
    abstract fun tabGroupDao(): TabGroupDao
    abstract fun shieldTelemetryDao(): ShieldTelemetryDao

    companion object {
        @Volatile
        private var INSTANCE: SecureBrowserDatabase? = null

        fun getInstance(context: Context): SecureBrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SecureBrowserDatabase::class.java,
                    "secure_browser_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
