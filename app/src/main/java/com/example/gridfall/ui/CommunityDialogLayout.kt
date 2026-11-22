package com.example.gridfall.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gridfall.R
import com.example.gridfall.game.MapBlockDefinition
import com.example.gridfall.network.CommunityPublication

/** Bounded by actual dialog-window insets, including IME; only the body scrolls. */
@Composable
internal fun CommunityDialogLayout(
    title: String,
    busy: Boolean,
    onClose: () -> Unit,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.(previewHeight: Dp, poolHeight: Dp) -> Unit
) {
    Dialog(onDismissRequest = { if (!busy) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(12.dp).testTag("community-dialog-viewport"),
            contentAlignment = Alignment.Center
        ) {
            // Very short windows (landscape + keyboard) keep actions in the scrolling body.
            val inlineActions = maxHeight < 280.dp
            val previewHeight = if (maxHeight < 480.dp) 64.dp else 96.dp
            val poolHeight = (maxHeight * 0.20f).coerceIn(64.dp, 128.dp)
            Surface(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().heightIn(max = maxHeight * 0.90f)
                    .testTag("community-map-dialog"),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        TextButton(enabled = !busy, onClick = onClose) { Text("Close") }
                    }
                    Column(
                        Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
                            .testTag("community-dialog-body"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        content(previewHeight, poolHeight)
                        if (inlineActions) actions?.invoke()
                    }
                    if (!inlineActions && actions != null) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        actions()
                    }
                }
            }
        }
    }
}

@Composable
internal fun CommunityMapOverview(map: CommunityPublication, previewHeight: Dp) {
    Text(map.document.name, style = MaterialTheme.typography.titleLarge)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SavedMapPreview(map.document.design, Modifier.size(previewHeight))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("By ${map.creator} · ${map.document.design.columns}×${map.document.design.rows}")
            Text("${map.document.design.blockPool.size} blocks · ${map.visibility} · unranked")
            Text("${String.format(java.util.Locale.ROOT, "%.1f", map.ratingAverage)} ★ (${map.ratingCount}) · ${map.downloads} imports")
            Text(map.code, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun CommunityBlockPool(blocks: List<MapBlockDefinition>, height: Dp) {
    Text("Available pieces", style = MaterialTheme.typography.titleSmall)
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = height).testTag("community-piece-list"),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(blocks, key = { it.id }) { block ->
            Row(Modifier.fillMaxWidth().testTag("community-piece-${block.id}"),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MapBlockPreview(block, Modifier.size(32.dp))
                Text("${block.name}: ${block.spawnChanceTenthsPercent / 10.0}%", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun CommunityRatings(rating: Int, enabled: Boolean, onRate: (Int) -> Unit) {
    Text("Rate this map", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth().testTag("community-rating-row"), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..5).forEach { stars ->
            TextButton(
                enabled = enabled,
                onClick = { onRate(stars) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("community-rating-$stars").semantics {
                    contentDescription = "$stars out of 5 stars"
                    selected = rating == stars
                },
                contentPadding = PaddingValues(2.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (rating == stars) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color.Transparent)
            ) {
                Text(stars.toString(), maxLines = 1, textAlign = TextAlign.Center)
                Icon(painterResource(R.drawable.ic_rating_star), contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}
