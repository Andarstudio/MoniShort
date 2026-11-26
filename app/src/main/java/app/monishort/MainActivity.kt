package app.monishort

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import app.monishort.core.theme.MoniTheme
import app.monishort.feature.MoniApp
import app.monishort.system.AutomationService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = MoniShortApp.container(this)
        lifecycleScope.launch {
            if (container.settings.engineEnabled.first()) AutomationService.start(this@MainActivity)
        }
        setContent { MoniTheme { MoniApp(container) } }
    }
}
