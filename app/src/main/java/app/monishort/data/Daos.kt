package app.monishort.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ShortcutEntity>>

    @Query("SELECT * FROM shortcuts WHERE lastRunAt IS NOT NULL ORDER BY lastRunAt DESC LIMIT :limit")
    fun observeRecentlyRun(limit: Int): Flow<List<ShortcutEntity>>

    @Query("SELECT * FROM shortcuts WHERE id = :id")
    suspend fun get(id: String): ShortcutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: ShortcutEntity)

    @Query("UPDATE shortcuts SET lastRunAt = :now, runCount = runCount + 1 WHERE id = :id")
    suspend fun markRun(id: String, now: Long)

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<AutomationEntity>>

    @Query("SELECT COUNT(*) FROM automations WHERE enabled = 1")
    fun observeEnabledCount(): Flow<Int>

    @Query("SELECT * FROM automations WHERE enabled = 1 AND triggerType = :type")
    suspend fun enabledForTrigger(type: String): List<AutomationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: AutomationEntity)

    @Query("UPDATE automations SET enabled = :enabled, updatedAt = :now WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, now: Long)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM execution_history ORDER BY id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExecutionHistoryEntity>>

    @Query("SELECT COUNT(*) FROM execution_history")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM execution_history WHERE status = 'SUCCESS'")
    fun observeSuccessCount(): Flow<Int>

    @Insert
    suspend fun insert(e: ExecutionHistoryEntity): Long

    @Query("DELETE FROM execution_history WHERE id NOT IN (SELECT id FROM execution_history ORDER BY id DESC LIMIT 500)")
    suspend fun prune()

    @Query("DELETE FROM execution_history")
    suspend fun clear()
}
