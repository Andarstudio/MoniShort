package app.monishort.feature.charging

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.monishort.AppContainer
import app.monishort.system.AutomationService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: ChargingViewModel = viewModel(factory = ChargingViewModel.factory(container))
    val s by vm.settings.collectAsState()
    val message by vm.message.collectAsState()
    val running by AutomationService.isRunning.collectAsState()
    val ctx = LocalContext.current
    var volume by remember(s.volumePercent) { mutableFloatStateOf(s.volumePercent.toFloat()) }

    val connectPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.pickSound(ctx, uri, connect = true)
    }
    val disconnectPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.pickSound(ctx, uri, connect = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charging sounds") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (running) "Background monitor is running" else "Background monitor is stopped",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        if (running) "Plug in your charger to hear the sound."
                        else "Turn it on in Settings, otherwise Android will not tell MoniShort about the charger.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            SwitchRow("Enable charging sounds", "Master switch for sounds and notifications below", s.enabled) { on ->
                vm.update { it.copy(enabled = on) }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("When charging starts", style = MaterialTheme.typography.titleSmall)
                    SoundRow(
                        name = s.connectSoundName,
                        onPick = { connectPicker.launch(arrayOf("audio/*")) },
                        onPreview = { vm.preview(s.connectSoundUri) },
                        onStop = vm::stopPreview,
                        onClear = { vm.clearSound(true) },
                    )
                    SwitchRow("Show notification", null, s.notifyOnConnect) { on -> vm.update { it.copy(notifyOnConnect = on) } }
                    OutlinedButton(onClick = { vm.test(true) }) { Text("Test connect event") }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("When charging stops", style = MaterialTheme.typography.titleSmall)
                    SwitchRow("Play a sound", null, s.disconnectSoundEnabled) { on -> vm.update { it.copy(disconnectSoundEnabled = on) } }
                    SoundRow(
                        name = s.disconnectSoundName,
                        onPick = { disconnectPicker.launch(arrayOf("audio/*")) },
                        onPreview = { vm.preview(s.disconnectSoundUri) },
                        onStop = vm::stopPreview,
                        onClear = { vm.clearSound(false) },
                    )
                    SwitchRow("Show notification", null, s.notifyOnDisconnect) { on -> vm.update { it.copy(notifyOnDisconnect = on) } }
                    OutlinedButton(onClick = { vm.test(false) }) { Text("Test disconnect event") }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Volume: ${volume.toInt()}%", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        onValueChangeFinished = { vm.update { it.copy(volumePercent = volume.toInt()) } },
                        valueRange = 0f..100f,
                    )
                    Text(
                        "Sounds play on the notification volume stream, so they follow your phone's notification volume and Do Not Disturb.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SoundRow(name: String?, onPick: () -> Unit, onPreview: () -> Unit, onStop: () -> Unit, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name ?: "No sound selected", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = onPick) { Text(if (name == null) "Choose file" else "Change") }
            if (name != null) {
                TextButton(onClick = onPreview) { Text("Preview") }
                TextButton(onClick = onStop) { Text("Stop") }
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
    }
}
