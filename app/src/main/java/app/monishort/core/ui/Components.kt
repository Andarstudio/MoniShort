package app.monishort.core.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.monishort.automation.ActionRegistry
import app.monishort.automation.ParamKind
import app.monishort.automation.ParamSpec
import app.monishort.automation.Step
import app.monishort.media.SoundFiles
import java.util.UUID

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Filled.Bolt) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
fun DropdownField(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${options.getOrNull(selected) ?: "-"}", maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(i); open = false })
            }
        }
    }
}

@Composable
fun ParamFields(specs: List<ParamSpec>, values: MutableMap<String, String>) {
    specs.forEach { p ->
        if (p.kind == ParamKind.URI && p.key == "uri") {
            AudioFileParamField(p, values)
        } else {
            OutlinedTextField(
                value = values[p.key].orEmpty(),
                onValueChange = { values[p.key] = it },
                label = { Text(p.label + if (p.required) " *" else "") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AudioFileParamField(spec: ParamSpec, values: MutableMap<String, String>) {
    val context = LocalContext.current
    val selectedUri = values[spec.key]?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    var pickerError by remember { mutableStateOf<String?>(null) }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                values[spec.key] = uri.toString()
                pickerError = null
            } catch (e: SecurityException) {
                pickerError = "MoniShort couldn't keep access to this file. Please choose it again."
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            spec.label + if (spec.required) " *" else "",
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    selectedUri?.let { SoundFiles.displayName(context, it) } ?: "No MP3 selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedUri == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
            }
            OutlinedButton(onClick = { audioPicker.launch(arrayOf("audio/mpeg")) }) {
                Text(if (selectedUri == null) "Choose MP3" else "Change MP3")
            }
        }
        pickerError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Picks one registered action and fills its parameters. Reports the resulting step on every change. */
@Composable
fun ActionForm(registry: ActionRegistry, onChange: (Step.Action) -> Unit) {
    val defs = remember { registry.all() }
    var idx by remember { mutableIntStateOf(0) }
    val params = remember { mutableStateMapOf<String, String>() }
    val stepId = remember { UUID.randomUUID().toString() }
    val def = defs[idx]
    LaunchedEffect(idx, params.toMap()) { onChange(Step.Action(stepId, def.type, params.toMap())) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DropdownField("Action", defs.map { "${it.category} · ${it.title}" }, idx) { idx = it; params.clear() }
        ParamFields(def.parameters, params)
    }
}
