package app.monishort.feature.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.monishort.AppContainer
import app.monishort.automation.Step
import app.monishort.automation.Workflow
import app.monishort.automation.WorkflowJson
import app.monishort.automation.WorkflowValidator
import app.monishort.core.ui.ActionForm
import app.monishort.core.ui.DropdownField
import app.monishort.core.ui.EmptyState
import app.monishort.core.ui.ParamFields
import app.monishort.data.AutomationEntity
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun AutomationScreen(container: AppContainer) {
    val flow = remember { container.db.automations().observeAll() }
    val automations by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<AutomationEntity?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (automations.isEmpty()) {
            EmptyState(
                "No automations yet",
                "Tap + to run an action when the charger is connected, the battery level changes, or the phone boots.",
                Modifier.align(Alignment.Center),
                icon = Icons.Filled.AutoMode,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(automations, key = { it.id }) { a ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(a.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    container.triggers.get(a.triggerType)?.title ?: a.triggerType,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(
                                checked = a.enabled,
                                onCheckedChange = { on -> scope.launch { container.db.automations().setEnabled(a.id, on, System.currentTimeMillis()) } },
                            )
                            IconButton(onClick = { deleting = a }) { Icon(Icons.Filled.Delete, "Delete ${a.name}") }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { creating = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, "New automation")
        }
    }

    if (creating) {
        val triggers = remember { container.triggers.all() }
        var name by remember { mutableStateOf("") }
        var tIdx by remember { mutableIntStateOf(0) }
        val config = remember { mutableStateMapOf<String, String>() }
        var step by remember { mutableStateOf<Step.Action?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val trigger = triggers[tIdx]
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("New automation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    DropdownField("When", triggers.map { it.title }, tIdx) { tIdx = it; config.clear() }
                    Text(trigger.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ParamFields(trigger.configFields, config)
                    Text("Then", style = MaterialTheme.typography.labelLarge)
                    ActionForm(container.actions) { step = it }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val s = step
                    val wf = Workflow(listOfNotNull(s))
                    val problems = WorkflowValidator.validate(wf, container.actions) + trigger.validate(config.toMap())
                    when {
                        name.isBlank() -> error = "Give the automation a name"
                        s == null -> error = "Choose an action"
                        problems.isNotEmpty() -> error = problems.joinToString("\n")
                        else -> scope.launch {
                            val now = System.currentTimeMillis()
                            container.db.automations().upsert(
                                AutomationEntity(
                                    id = UUID.randomUUID().toString(), name = name.trim(),
                                    triggerType = trigger.type, triggerConfigJson = WorkflowJson.encodeMap(config.toMap()),
                                    workflowJson = WorkflowJson.encode(wf), enabled = true, createdAt = now, updatedAt = now,
                                )
                            )
                            creating = false
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancel") } },
        )
    }

    deleting?.let { a ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete automation?") },
            text = { Text("“${a.name}” will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { scope.launch { container.db.automations().delete(a.id); deleting = null } }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}
