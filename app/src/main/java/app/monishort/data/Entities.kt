package app.monishort.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String = "bolt",
    val color: Int = 0xFF5B4BDB.toInt(),
    val folderId: String? = null,
    val workflowJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastRunAt: Long? = null,
    val runCount: Int = 0,
)

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val triggerType: String,
    val triggerConfigJson: String = "{}",
    val workflowJson: String,
    val enabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val description: String,
    val workflowJson: String,
    val createdAt: Long,
)

@Entity(tableName = "execution_history")
data class ExecutionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val executionId: String,
    val name: String,
    val status: String,
    val message: String,
    val startedAt: Long,
    val finishedAt: Long,
    val log: String,
)
