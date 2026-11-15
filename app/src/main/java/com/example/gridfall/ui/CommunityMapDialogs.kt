package com.example.gridfall.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.*
import com.example.gridfall.network.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.gridfall.ui.theme.LocalGridfallColors

@Composable
internal fun CommunityMapsTheme(content: @Composable () -> Unit) {
    val c = LocalGridfallColors.current
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = c.accentStrong, secondary = c.accent,
        surface = c.dialogBackground, background = c.backgroundBottom, onSurface = c.textPrimary,
        onBackground = c.textPrimary, onPrimary = c.backgroundBottom, error = c.danger), content = content)
}

/** Sharing stays separate from the editor draft and never starts a ranked run. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CommunityMapDialog(savedMap: SavedCustomMap?, client: CommunityMapsGateway, eligible: Boolean,
    token: suspend () -> String, onImport: (CommunityPublication, Boolean) -> Unit, onDismiss: () -> Unit,
    initialCode: String? = null) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("unlisted") }
    var publication by remember { mutableStateOf<CommunityPublication?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rating by remember { mutableStateOf(0) }
    var showReport by remember { mutableStateOf(false) }
    var reportNotes by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val requestId = remember { UUID.randomUUID().toString() }
    val importId = remember { UUID.randomUUID().toString() }
    fun runAction(action: suspend () -> Unit) { scope.launch {
        busy = true; error = null
        try { action() } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "Map action failed." }
        finally { busy = false }
    } }
    LaunchedEffect(initialCode) { if (initialCode != null) runAction { publication = client.fetch(initialCode) } }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(if (initialCode != null) "Community Map" else if (savedMap == null) "Enter Map Code" else "Share Map") },
        text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (publication == null) {
                if (savedMap == null) {
                    OutlinedTextField(code, { code = it.take(32) }, label = { Text("Map code") }, singleLine = true)
                    Button(onClick = { runAction { publication = client.fetch(code) } }, enabled = !busy) { Text("Preview") }
                } else {
                    Text(savedMap.name)
                    Text("Publish an immutable copy. Editing your saved map later creates a new publication.")
                    OutlinedTextField(description, { description = it.take(500) }, enabled = !busy, label = { Text("Description (optional)") })
                    Row { RadioButton(visibility == "unlisted", { visibility = "unlisted" }); Text("Unlisted · code only") }
                    Row { RadioButton(visibility == "public", { visibility = "public" }); Text("Public · marketplace") }
                    if (!eligible) Text("Sign in and choose a username in Settings before publishing.")
                    Button(enabled = eligible && !busy, onClick = { runAction {
                        publication = client.publish(token(), CommunityMapFormat.export(savedMap.name, description, savedMap.design), visibility, requestId)
                    } }) { Text("Publish Map") }
                }
            }
            publication?.let { map ->
                Text(map.document.name, style = MaterialTheme.typography.titleLarge)
                SavedMapPreview(map.document.design, Modifier.fillMaxWidth().height(140.dp))
                Text("By ${map.creator} · ${map.document.design.columns}×${map.document.design.rows}")
                Text("${map.document.design.blockPool.size} blocks · ${map.visibility} · unranked")
                Text("${String.format(java.util.Locale.ROOT, "%.1f", map.ratingAverage)} ★ (${map.ratingCount}) · ${map.downloads} imports")
                if (map.document.description.isNotEmpty()) Text(map.document.description)
                Text(map.code)
                TextButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Gridfall map code", map.code))
                }) { Text("Copy Code") }
                if (savedMap == null) {
                    Text(map.document.design.blockPool.joinToString("\n") { "${it.name}: ${it.spawnChanceTenthsPercent / 10.0}%" })
                    Button(enabled = !busy, onClick = { runAction { onImport(client.importMap(map.code, importId, if (eligible) token() else null), false) } }) { Text("Save Map Offline") }
                    Button(enabled = !busy, onClick = { runAction { onImport(client.importMap(map.code, importId, if (eligible) token() else null), true) } }) { Text("Import & Play") }
                    Text("Rate this map", style = MaterialTheme.typography.titleSmall)
                    FlowRow {
                        (1..5).forEach { stars ->
                            TextButton(enabled = eligible && !busy, onClick = { runAction {
                                publication = client.rate(map.code, stars, token()); rating = stars; notice = "Rating saved."
                            } }) { Text("$stars ${if (rating == stars) "★" else "☆"}") }
                        }
                    }
                    if (!eligible) Text("Sign in with a username to rate or report maps.")
                    TextButton(enabled = eligible && !busy, onClick = { showReport = !showReport }) { Text("Report Map") }
                    if (showReport) {
                        OutlinedTextField(reportNotes, { reportNotes = it.take(500) }, label = { Text("Report details (optional)") })
                        FlowRow { listOf("inappropriate", "spam", "other").forEach { reason ->
                            TextButton(enabled = !busy, onClick = { runAction {
                                client.report(map.code, reason, reportNotes, token()); showReport = false; notice = "Report sent for review."
                            } }) { Text(reason.replaceFirstChar(Char::uppercase)) }
                        } }
                    }
                }
            }
            if (busy) CircularProgressIndicator()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            notice?.let { Text(it) }
        } }, confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") } })
}
