package app.monishort.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema version 1. When the schema changes, bump [version] and add a Migration in AppContainer
 * (never use destructive migration - user workflows must survive updates).
 */
@Database(
    entities = [
        ShortcutEntity::class,
        AutomationEntity::class,
        FolderEntity::class,
        TemplateEntity::class,
        ExecutionHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MoniDatabase : RoomDatabase() {
    abstract fun shortcuts(): ShortcutDao
    abstract fun automations(): AutomationDao
    abstract fun folders(): FolderDao
    abstract fun templates(): TemplateDao
    abstract fun history(): HistoryDao
}
