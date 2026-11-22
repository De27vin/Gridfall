package com.example.gridfall.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.*
import com.example.gridfall.network.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.gridfall.ui.theme.LocalGridfallColors
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role

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
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var code by remember { mutableStateOf("") }
    var description by remember { mutableStateOf(savedMap?.description.orEmpty()) }
    var visibility by remember { mutableStateOf("unlisted") }
    var publication by remember { mutableStateOf<CommunityPublication?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rating by remember { mutableStateOf(0) }
    var showReport by remember { mutableStateOf(false) }
    var reportNotes by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var pendingDraft by remember { mutableStateOf<PendingPublication?>(null) }
    val requestId = rememberSaveable(savedMap?.id, initialCode) { UUID.randomUUID().toString() }
    val importId = rememberSaveable(savedMap?.id, initialCode) { UUID.randomUUID().toString() }
    fun runAction(action: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        focusManager.clearFocus(); keyboard?.hide(); scope.launch {
        try { action() } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "Map action failed." }
        finally { busy = false }
    } }
    LaunchedEffect(initialCode) { if (initialCode != null) runAction { publication = client.fetch(initialCode) } }
    LaunchedEffect(savedMap?.id, eligible) {
        if (savedMap != null && eligible) runAction {
            pendingDraft = client.pendingPublication(token(), savedMap.id)
            pendingDraft?.let {
                description = CommunityMapFormat.decode(it.document).description
                visibility = it.visibility
            }
        }
    }
    fun close() { if (savedMap != null) publication?.let { client.finishPublication(it.code) }; onDismiss() }
    CommunityDialogLayout(
        title = if (initialCode != null) "Community Map" else if (savedMap == null) "Enter Map Code" else "Share Map",
        busy = busy, onClose = ::close,
        actions = if (publication != null && savedMap == null) ({
            val map = publication!!
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f).heightIn(min = 48.dp), enabled = !busy, onClick = {
                    runAction { onImport(client.importMap(map.code, importId, if (eligible) token() else null), false) }
                }) { Text("Save Map Offline", textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                Button(modifier = Modifier.weight(1f).heightIn(min = 48.dp), enabled = !busy, onClick = {
                    runAction { onImport(client.importMap(map.code, importId, if (eligible) token() else null), true) }
                }) { Text("Import & Play", textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
        }) else null
    ) { previewHeight, poolHeight ->
            if (publication == null) {
                if (savedMap == null) {
                    OutlinedTextField(code, { code = it.take(32) }, label = { Text("Map code") }, singleLine = true)
                    Button(onClick = { runAction { publication = client.fetch(code) } }, enabled = !busy) { Text("Preview") }
                } else {
                    Text(pendingDraft?.let { CommunityMapFormat.decode(it.document).name } ?: savedMap.name)
                    if (pendingDraft != null) Text("A previous publication has an uncertain outcome. Retry its saved snapshot before starting a new publication.")
                    Text("Publish an immutable copy. Editing your saved map later creates a new publication.")
                    OutlinedTextField(description, { description = it.take(500) }, enabled = !busy && pendingDraft == null, label = { Text("Description (optional)") })
                    Row(Modifier.fillMaxWidth().selectable(visibility == "unlisted", enabled = !busy && pendingDraft == null, role = Role.RadioButton,
                        onClick = { visibility = "unlisted" })) { RadioButton(visibility == "unlisted", null); Text("Unlisted · code only") }
                    Row(Modifier.fillMaxWidth().selectable(visibility == "public", enabled = !busy && pendingDraft == null, role = Role.RadioButton,
                        onClick = { visibility = "public" })) { RadioButton(visibility == "public", null); Text("Public · marketplace") }
                    if (!eligible) Text("Sign in and choose a username in Settings before publishing.")
                    Button(enabled = eligible && !busy, onClick = { runAction {
                        val identity = token()
                        try {
                            publication = client.publishSavedMap(identity, savedMap.id,
                                pendingDraft?.document ?: CommunityMapFormat.export(savedMap.name, description, savedMap.design), visibility, requestId)
                        } finally {
                            pendingDraft = client.pendingPublication(identity, savedMap.id)
                        }
                    } }) { Text("Publish Map") }
                }
            }
            publication?.let { map ->
                CommunityMapOverview(map, previewHeight)
                TextButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Gridfall map code", map.code))
                }) { Text("Copy Code") }
                if (savedMap != null && map.document.description.isNotEmpty()) Text(map.document.description)
                if (savedMap == null) {
                    CommunityRatings(rating, eligible && !busy) { stars -> runAction {
                        publication = client.rate(map.code, stars, token()); rating = stars; notice = "Rating saved."
                    } }
                    CommunityBlockPool(map.document.design.blockPool, poolHeight)
                    if (map.document.description.isNotEmpty()) Text(map.document.description)
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
    }
}
