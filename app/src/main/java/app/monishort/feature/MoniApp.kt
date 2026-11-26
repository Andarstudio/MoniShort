package app.monishort.feature

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.monishort.AppContainer
import app.monishort.feature.automation.AutomationScreen
import app.monishort.feature.charging.ChargingScreen
import app.monishort.feature.gallery.GalleryScreen
import app.monishort.feature.home.HomeScreen
import app.monishort.feature.settings.SettingsScreen
import app.monishort.feature.shortcuts.ShortcutsScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Shortcuts("Shortcuts", Icons.Filled.Bolt),
    Automation("Automation", Icons.Filled.AutoMode),
    Gallery("Gallery", Icons.Filled.GridView),
    Settings("Settings", Icons.Filled.Settings),
}

@Composable
fun MoniApp(container: AppContainer) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var showCharging by rememberSaveable { mutableStateOf(false) }

    if (showCharging) {
        BackHandler { showCharging = false }
        ChargingScreen(container, onBack = { showCharging = false })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.Home -> HomeScreen(container, onOpenCharging = { showCharging = true })
                Tab.Shortcuts -> ShortcutsScreen(container)
                Tab.Automation -> AutomationScreen(container)
                Tab.Gallery -> GalleryScreen()
                Tab.Settings -> SettingsScreen(container, onOpenCharging = { showCharging = true })
            }
        }
    }
}
