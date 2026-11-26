package app.monishort.feature.shortcuts

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import app.monishort.core.ui.EmptyState
import app.monishort.data.ShortcutEntity
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun ShortcutsScreen(container: AppContainer) {
    val flow = remember { container.db.shortcuts().observeAll() }
    val shortcuts by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ShortcutEntity?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (shortcuts.isEmpty()) {
            EmptyState(
                "No shortcuts yet",
                "Tap + to create your first shortcut. The full multi-step visual editor is the next development step.",
                Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(shortcuts, key = { it.id }) { s ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.name, style = MaterialTheme.typography.titleMedium)
                                Text("${s.runCount} runs", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        val wf = WorkflowJson.decode(s.workflowJson)
                                        container.engine.start(s.name, wf, container.platform)
                                        container.db.shortcuts().markRun(s.id, System.currentTimeMillis())
                                    } catch (e: Exception) { }
                                }
                            }) { Icon(Icons.Filled.PlayArrow, "Run ${s.name}") }
                            IconButton(onClick = { deleting = s }) { Icon(Icons.Filled.Delete, "Delete ${s.name}") }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { creating = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, "New shortcut")
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        var step by remember { mutableStateOf<Step.Action?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("New shortcut") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    ActionForm(container.actions) { step = it }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val s = step
                    val wf = Workflow(listOfNotNull(s))
                    val problems = WorkflowValidator.validate(wf, container.actions)
                    when {
                        name.isBlank() -> error = "Give the shortcut a name"
                        s == null -> error = "Choose an action"
                        problems.isNotEmpty() -> error = problems.joinToString("\n")
                        else -> scope.launch {
                            val now = System.currentTimeMillis()
                            container.db.shortcuts().upsert(
                                ShortcutEntity(
                                    id = UUID.randomUUID().toString(), name = name.trim(),
                                    workflowJson = WorkflowJson.encode(wf), createdAt = now, updatedAt = now,
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

    deleting?.let { s ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete shortcut?") },
            text = { Text("“${s.name}” will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { scope.launch { container.db.shortcuts().delete(s.id); deleting = null } }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}
