package app.monishort.feature.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.monishort.AppContainer
import app.monishort.core.ui.SectionTitle
import app.monishort.system.AutomationService
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, onOpenCharging: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val engineOn by container.settings.engineEnabled.collectAsState(initial = true)
    val running by AutomationService.isRunning.collectAsState()
    val startError by AutomationService.lastStartError.collectAsState()
    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(ctx).areNotificationsEnabled()) }

    LifecycleResumeEffect(Unit) {
        notificationsOn = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        onPauseOrDispose { }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsOn = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    fun openSafely(intent: Intent) {
        try { ctx.startActivity(intent) } catch (e: Exception) { }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SectionTitle("Automation engine")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Background monitor", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (running) "Running — listening for charger and battery events"
                            else "Stopped — triggers will not fire",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = engineOn, onCheckedChange = { on ->
                        scope.launch { container.settings.setEngineEnabled(on) }
                        if (on) AutomationService.start(ctx) else AutomationService.stop(ctx)
                    })
                }
                startError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text(
                    "Android needs this monitor (with its persistent notification) to deliver charger and battery events.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionTitle("Permissions")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notifications", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (notificationsOn) "Allowed" else "Not allowed — notification actions and charging notifications will fail.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!notificationsOn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Build.VERSION.SDK_INT >= 33) {
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow") }
                        }
                        OutlinedButton(onClick = {
                            openSafely(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                            )
                        }) { Text("Open settings") }
                    }
                }
            }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Battery optimization", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Some phones stop background services to save power. If charging sounds stop working, " +
                        "exclude MoniShort from battery optimization and enable Autostart if your phone has it.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { openSafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }) { Text("Battery settings") }
                    OutlinedButton(onClick = {
                        openSafely(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${ctx.packageName}"))
                        )
                    }) { Text("App info") }
                }
            }
        }

        SectionTitle("Features")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Charging sounds", style = MaterialTheme.typography.titleSmall)
                    Text("Play a custom sound when charging starts or stops", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onOpenCharging) { Text("Open") }
            }
        }

        SectionTitle("About MoniShort")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("MoniShort 0.1.0", style = MaterialTheme.typography.titleSmall)
                Text(
                    "No analytics and no hidden data collection. Data stays on this device unless a shortcut you create sends it somewhere.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
