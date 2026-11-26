package app.monishort.feature.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.monishort.core.ui.EmptyState

@Composable
fun GalleryScreen() {
    Box(Modifier.fillMaxSize()) {
        EmptyState(
            "Gallery is coming next",
            "Built-in templates you can import into your shortcuts arrive together with the visual workflow editor.",
            Modifier.align(Alignment.Center),
            icon = Icons.Filled.GridView,
        )
    }
}
