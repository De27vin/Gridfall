package com.example.gridfall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gridfall.network.*
import com.example.gridfall.ui.theme.LocalGridfallColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityMapsScreen(client: CommunityMapsGateway, eligible: Boolean, token: suspend () -> String,
    onImport: (CommunityPublication, Boolean) -> Unit, onClose: () -> Unit) {
    val colors = LocalGridfallColors.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("rated") }
    var mine by rememberSaveable { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var items by remember { mutableStateOf(emptyList<CommunityPublication>()) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var unpublish by remember { mutableStateOf<CommunityPublication?>(null) }
    var loadGeneration by remember { mutableIntStateOf(0) }
    var unpublishBusy by remember { mutableStateOf(false) }
    var unpublishError by remember { mutableStateOf<String?>(null) }
    var locallyUnpublished by remember { mutableStateOf(emptySet<String>()) }
    suspend fun load(append: Boolean) {
        if (append && loading) return
        val generation = ++loadGeneration
        loading = true; error = null
        try {
            val requestedQuery = submitted
            val requestedSort = sort
            val requestedMine = mine
            val ownerToken = if (requestedMine) token() else null
            val page = loadVisibleCommunityPage(
                cursor = if (append) cursor else null,
                publishedOnly = requestedMine,
                excludedCodes = locallyUnpublished,
                onSkippedPage = { if (generation == loadGeneration) cursor = it }
            ) { next ->
                if (generation != loadGeneration) throw CancellationException("Obsolete community request")
                client.browse(requestedQuery, requestedSort, next, ownerToken).also {
                    if (generation != loadGeneration) throw CancellationException("Obsolete community request")
                }
            }
            if (generation != loadGeneration) return
            items = ((if (append) items else emptyList()) + page.items).distinctBy { it.code }
            cursor = page.nextCursor
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { if (generation == loadGeneration) error = e.message ?: "Could not load maps." }
        finally { if (generation == loadGeneration) loading = false }
    }
    fun startUnpublish(map: CommunityPublication) {
        if (unpublishBusy) return
        unpublishBusy = true
        unpublishError = null
        scope.launch {
            try {
                client.unpublish(map.code, token())
                locallyUnpublished = locallyUnpublished + map.code
                items = items.filterNot { it.code == map.code }
                // An older page may have been fetched before the DELETE committed.
                loadGeneration++
                unpublish = null
                refresh++
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                unpublishError = e.message ?: "Unpublish failed. Please retry."
            } finally { unpublishBusy = false }
        }
    }
    LaunchedEffect(submitted, sort, mine, refresh) { items = emptyList(); cursor = null; load(false) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(color = colors.backgroundBottom, contentColor = colors.textPrimary,
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().safeDrawingPadding().imePadding()) {
            LazyColumn(Modifier.fillMaxSize().testTag("community-maps"), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    TextButton(onClick = onClose) { Text("Back", color = colors.textSecondary) }
                    Text(if (mine) "My Published Maps" else "Community Maps", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    Text("Discover new challenges · all maps are unranked", color = colors.textMuted)
                }
                item {
                    OutlinedTextField(query, { query = it.take(80) }, label = { Text("Search maps or creators") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus(); keyboard?.hide(); submitted = query.trim(); refresh++ }))
                    FlowRow {
                        TextButton(onClick = { focusManager.clearFocus(); keyboard?.hide(); submitted = query.trim(); refresh++ }, enabled = !loading) { Text("Search") }
                        TextButton(onClick = { mine = !mine }, enabled = eligible && !loading) { Text(if (mine) "Browse All Maps" else "My Published Maps") }
                        TextButton(onClick = { refresh++ }, enabled = !loading) { Text("Refresh") }
                    }
                    if (!eligible) Text("Guests can browse and import. Sign in with a username to publish or rate.", color = colors.textMuted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("rated" to "Top Rated", "popular" to "Popular", "downloads" to "Most Downloaded", "newest" to "Newest").forEach { (key, label) ->
                            FilterChip(selected = sort == key, onClick = { if (!loading) sort = key }, label = { Text(label) })
                        }
                    }
                }
                if (!loading && items.isEmpty() && cursor == null && error == null) item {
                    Text(if (mine) {
                        if (submitted.isNotBlank()) "No published maps match this search."
                        else "You have no published maps. Share one from Saved Maps."
                    } else "No maps found. Try another search.", color = colors.textMuted)
                }
                items(items, key = { it.code }) { map ->
                    Column(Modifier.fillMaxWidth().border(1.dp, colors.panelBorder, RoundedCornerShape(12.dp))
                        .background(colors.chipBackground, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Row(Modifier.fillMaxWidth().clickable(enabled = map.published && !map.hidden) { detail = map.code }, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SavedMapPreview(map.document.design, Modifier.size(64.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(map.document.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                Text("By ${map.creator}", color = colors.textSecondary)
                                Text("${map.document.design.columns}×${map.document.design.rows} · ${String.format(Locale.ROOT, "%.1f", map.ratingAverage)} ★ (${map.ratingCount})", color = colors.textMuted)
                                Text("${map.downloads} imports", color = colors.textMuted)
                            }
                        }
                        FlowRow {
                            TextButton(enabled = map.published && !map.hidden, onClick = { detail = map.code }) { Text("Preview / Play") }
                            if (mine) {
                                Text("${map.visibility} · ${if (map.hidden) "Hidden" else "Published"}", modifier = Modifier.padding(8.dp))
                                if (map.published) TextButton(modifier = Modifier.testTag("community-unpublish-${map.code}"), enabled = !loading && !unpublishBusy,
                                    onClick = { unpublishError = null; unpublish = map }) { Text("Unpublish", color = colors.danger) }
                            }
                        }
                    }
                }
                if (loading) item { CircularProgressIndicator(Modifier.testTag("community-loading")) }
                error?.let { message -> item {
                    Text(message, color = colors.danger)
                    Button(onClick = { scope.launch { load(cursor != null || items.isNotEmpty()) } }, enabled = !loading) { Text("Retry") }
                    Text("Previously imported maps are available offline in Saved Maps.", color = colors.textMuted)
                } }
                if (cursor != null && !loading && error == null) item { Button(onClick = { scope.launch { load(true) } }) { Text("Load More") } }
            }
        }
    }
    detail?.let { code -> CommunityMapDialog(null, client, eligible, token,
        onImport = { publication, play -> detail = null; onImport(publication, play) }, onDismiss = { detail = null }, initialCode = code) }
    unpublish?.let { map -> AlertDialog(
        onDismissRequest = { if (!unpublishBusy) unpublish = null },
        title = { Text("Unpublish map?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${map.document.name} will no longer be available online. Existing offline imports stay playable.")
            if (unpublishBusy) CircularProgressIndicator(Modifier.size(24.dp))
            unpublishError?.let { Text(it, color = colors.danger, modifier = Modifier.testTag("community-unpublish-error")) }
        } },
        confirmButton = { TextButton(modifier = Modifier.testTag("community-confirm-unpublish"), enabled = !unpublishBusy,
            onClick = { startUnpublish(map) }) { Text("Unpublish") } },
        dismissButton = { TextButton(enabled = !unpublishBusy, onClick = { unpublish = null }) { Text("Cancel") } }
    ) }
}
