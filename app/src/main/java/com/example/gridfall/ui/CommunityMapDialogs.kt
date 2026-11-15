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

/** Sharing stays separate from the editor draft and never starts a ranked run. */
@Composable
fun CommunityMapDialog(savedMap: SavedCustomMap?, client: CommunityMapsClient, eligible: Boolean,
    token: suspend () -> String, onImport: (CommunityPublication, Boolean) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("unlisted") }
    var publication by remember { mutableStateOf<CommunityPublication?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val requestId = remember { UUID.randomUUID().toString() }
    fun runAction(action: suspend () -> Unit) { scope.launch {
        busy = true; error = null
        try { action() } catch (e: Exception) { error = e.message ?: "Map action failed." }
        finally { busy = false }
    } }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(if (savedMap == null) "Enter Map Code" else "Share Map") },
        text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (publication == null) {
                if (savedMap == null) {
                    OutlinedTextField(code, { code = it.take(32) }, label = { Text("Map code") }, singleLine = true)
                    Button(onClick = { runAction { publication = client.fetch(code) } }, enabled = !busy) { Text("Preview") }
                } else {
                    Text(savedMap.name)
                    Text("Publish an immutable copy. Editing your saved map later creates a new publication.")
                    OutlinedTextField(description, { description = it.take(500) }, label = { Text("Description (optional)") })
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
                if (map.document.description.isNotEmpty()) Text(map.document.description)
                Text(map.code)
                TextButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Gridfall map code", map.code))
                }) { Text("Copy Code") }
                if (savedMap == null) {
                    Text(map.document.design.blockPool.joinToString("\n") { "${it.name}: ${it.spawnChanceTenthsPercent / 10.0}%" })
                    Button(enabled = !busy, onClick = { runAction { onImport(client.fetch(map.code), false) } }) { Text("Save Map Offline") }
                    Button(enabled = !busy, onClick = { runAction { onImport(client.fetch(map.code), true) } }) { Text("Import & Play") }
                }
            }
            if (busy) CircularProgressIndicator()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }, confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") } })
}
