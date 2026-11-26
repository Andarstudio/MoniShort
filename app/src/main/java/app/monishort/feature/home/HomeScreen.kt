package app.monishort.feature.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.monishort.AppContainer
import app.monishort.automation.ExecutionStatus
import app.monishort.core.ui.EmptyState
import app.monishort.core.ui.SectionTitle
import app.monishort.data.ChargingSettings
import app.monishort.data.ExecutionHistoryEntity

@Composable
fun HomeScreen(container: AppContainer, onOpenCharging: () -> Unit) {
    val historyFlow = remember { container.db.history().observeRecent(10) }
    val countFlow = remember { container.db.history().observeCount() }
    val okFlow = remember { container.db.history().observeSuccessCount() }
    val activeFlow = remember { container.db.automations().observeEnabledCount() }
    val recentFlow = remember { container.db.shortcuts().observeRecentlyRun(5) }
    val history by historyFlow.collectAsState(initial = emptyList())
    val total by countFlow.collectAsState(initial = 0)
    val ok by okFlow.collectAsState(initial = 0)
    val active by activeFlow.collectAsState(initial = 0)
    val recent by recentFlow.collectAsState(initial = emptyList())
    val charging by container.settings.charging.collectAsState(initial = ChargingSettings())

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Welcome to MoniShort", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Automate your phone with shortcuts and triggers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Runs", total.toString(), Modifier.weight(1f))
                StatCard("Succeeded", ok.toString(), Modifier.weight(1f))
                StatCard("Active", active.toString(), Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Charging sounds", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (charging.enabled) "On — plays your sound when the charger is connected."
                        else "Off — choose a sound to play when charging starts or stops.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = onOpenCharging) { Text("Configure") }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item { SectionTitle("Recently run shortcuts") }
            items(recent, key = { it.id }) { s ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(s.name, style = MaterialTheme.typography.titleSmall)
                        Text("${s.runCount} runs", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { SectionTitle("Recent executions") }
        if (history.isEmpty()) {
            item {
                EmptyState(
                    "Nothing has run yet",
                    "Run a shortcut or let an automation trigger. Results appear here.",
                    icon = Icons.Filled.History,
                )
            }
        } else {
            items(history, key = { it.id }) { HistoryRow(it) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HistoryRow(e: ExecutionHistoryEntity) {
    val color = when (e.status) {
        ExecutionStatus.SUCCESS.name -> MaterialTheme.colorScheme.primary
        ExecutionStatus.FAILED.name -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(e.name, style = MaterialTheme.typography.titleSmall)
                Text(e.status.lowercase().replaceFirstChar { it.uppercase() }, color = color, style = MaterialTheme.typography.labelLarge)
            }
            Text(e.message, style = MaterialTheme.typography.bodySmall)
            Text(
                DateUtils.getRelativeTimeSpanString(e.finishedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
