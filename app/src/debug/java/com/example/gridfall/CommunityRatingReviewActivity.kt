package com.example.gridfall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.*
import com.example.gridfall.network.*
import com.example.gridfall.ui.*
import com.example.gridfall.ui.theme.GridfallTheme
import kotlinx.coroutines.delay
import java.io.IOException

/** Debug-only interactive fixture. No HTTP client, Firebase login or production mutations. */
class CommunityRatingReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent.getBooleanExtra("unpublish_review", false)) {
            openUnpublishReview()
            return
        }
        val preferences = getSharedPreferences("local_star_review", MODE_PRIVATE)
        val shareReview = intent.getBooleanExtra("share_review", false)
        val detailsReview = intent.getBooleanExtra("details_review", false)
        val fixture = CommunityPublication("GF-A7K9P23456", "Local review fixture",
            CommunityMapDocument(if (detailsReview) "LOCAL REVIEW · Map details" else "LOCAL REVIEW · Star rating",
                if (detailsReview) "Scroll inside Available pieces to see all 20 entries. The dialog header and bottom actions remain accessible." else "Local fixture only. Close and reopen to verify your saved stars.",
                CustomMapDesign(7, 7, setOf(Cell(0,0), Cell(6,6)), if (detailsReview) List(20) { index ->
                    MapBlockDefinition("review-$index", "Review piece ${index + 1}",
                        setOf(Cell(0,0), Cell(1,0)), 50, index % 5)
                } else listOf(
                    MapBlockDefinition("single", "Single", setOf(Cell(0,0)), 600, 2),
                    MapBlockDefinition("corner", "Corner", setOf(Cell(0,0),Cell(1,0),Cell(1,1)), 400, 1)))), true, "public", 12, 4.2, 8)
        val savedMap = SavedCustomMap("local-share-review", "LOCAL REVIEW · Share map", fixture.document.design)
        val existing = fixture.copy(code = "GF-B7K9P23456", visibility = "unlisted",
            document = fixture.document.copy(name = "LOCAL REVIEW · Existing unlisted map"))
        var lastSubmission by mutableStateOf("No publication submitted.")
        var failNext = false
        val gateway = object : CommunityMapsGateway {
            override suspend fun fetch(code: String) = if (code == existing.code) existing else fixture.copy(yourRating = preferences.getInt("rating", 0).takeIf { it in 1..5 })
            override suspend fun browse(query: String, sort: String, cursor: String?, ownerToken: String?) = CommunityPage(listOf(fixture), null)
            override suspend fun publish(token: String, document: String, visibility: String, requestId: String): CommunityPublication {
                check(shareReview)
                delay(300)
                lastSubmission = "Local submission: $visibility. Existing publication remains ${existing.visibility}."
                return fixture.copy(document = CommunityMapFormat.decode(document), visibility = visibility)
            }
            override suspend fun importMap(code: String, requestId: String, token: String?) = fixture
            override suspend fun rate(code: String, stars: Int, token: String): CommunityPublication {
                delay(400)
                if (failNext) { failNext = false; throw IOException("Local test: rating request failed. Your saved rating was restored.") }
                preferences.edit().putInt("rating", stars).apply()
                return fixture.copy(yourRating = stars)
            }
            override suspend fun unpublish(code: String, token: String) = Unit
            override suspend fun report(code: String, reason: String, notes: String, token: String) = Unit
        }
        setContent { GridfallTheme { CommunityMapsTheme {
            var viewExisting by remember { mutableStateOf(false) }
            var opened by remember { mutableStateOf(true) }
            var generation by remember { mutableIntStateOf(0) }
            var message by remember { mutableStateOf("No network or production accounts are used.") }
            Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (shareReview) "LOCAL REVIEW — Share map" else if (detailsReview) "LOCAL REVIEW — Map details" else "LOCAL REVIEW — Five-star rating", style = MaterialTheme.typography.titleLarge)
                    Text(message)
                    if (shareReview) {
                        Text(lastSubmission)
                        Text("Existing local publication: unlisted (immutable).")
                        Button(onClick = { viewExisting = false; generation++; opened = true }) { Text("New Share Map dialog") }
                        OutlinedButton(onClick = { viewExisting = true; generation++; opened = true }) { Text("View existing unlisted map") }
                    } else {
                    Button(onClick = { generation++; opened = true }) { Text("Reopen map") }
                    OutlinedButton(onClick = { failNext = true; message = "The next rating will fail once. Reopen the map and tap a star." }) { Text("Reject next rating") }
                    OutlinedButton(onClick = { preferences.edit().remove("rating").apply(); generation++; opened = true }) { Text("Reset to unrated") }
                    }
                }
            }
            if (opened) key(generation) { CommunityMapDialog(if (shareReview && !viewExisting) savedMap else null, gateway, true, { "local-fixture-only" },
                { _, _ -> opened = false; message = "Import actions are outside this rating-only review. Use the Gridfall launcher to play." }, { opened = false }, initialCode = if (shareReview && !viewExisting) null else if (viewExisting) existing.code else fixture.code) }
        } } }
    }
}

/** Persistent local gateway fixture: exercises the production screen, not a production API. */
private fun CommunityRatingReviewActivity.openUnpublishReview() {
    val preferences = getSharedPreferences("local_unpublish_review", android.content.Context.MODE_PRIVATE)
    val design = CustomMapDesign(7, 7, emptySet(), listOf(
        MapBlockDefinition("single", "Single", setOf(Cell(0, 0)), 1000, 2)))
    val draft = SavedCustomMap("local-unpublish-review-original", "LOCAL REVIEW · Original", design)
    val store = SavedCustomMapStore(this)
    if (store.load().none { it.id == draft.id }) store.save(draft)
    // Compare against the stored baseline, including the store's normal name normalization.
    val saved = store.load().first { it.id == draft.id }
    val success = CommunityPublication("GF-A7K9P23456", "Local fixture owner",
        CommunityMapDocument("LOCAL REVIEW · Successful unpublish", "Local data only", design), true, "public")
    val rejected = success.copy(code = "GF-B7K9P23456",
        document = success.document.copy(name = "LOCAL REVIEW · Rejected unpublish"))
    val gateway = object : CommunityMapsGateway {
        override suspend fun browse(query: String, sort: String, cursor: String?, ownerToken: String?): CommunityPage {
            delay(200)
            val rows = listOf(success.copy(published = preferences.getBoolean("published", true)), rejected)
                .filter { query.isBlank() || it.document.name.contains(query, true) }
                .filter { ownerToken != null || it.published }
            // Owner history intentionally includes unpublished rows, like the existing API.
            return CommunityPage(rows, null)
        }
        override suspend fun fetch(code: String) = if (code == rejected.code) rejected else success
        override suspend fun publish(token: String, document: String, visibility: String, requestId: String): CommunityPublication = error("Not part of this review")
        override suspend fun importMap(code: String, requestId: String, token: String?) = fetch(code)
        override suspend fun rate(code: String, stars: Int, token: String) = fetch(code)
        override suspend fun report(code: String, reason: String, notes: String, token: String) = Unit
        override suspend fun unpublish(code: String, token: String) {
            delay(800)
            if (code == rejected.code) throw IOException("Local test: unpublish rejected. The publication has been preserved.")
            check(code == success.code)
            check(preferences.edit().putBoolean("published", false).commit())
        }
    }
    setContent { GridfallTheme { CommunityMapsTheme {
        var opened by remember { mutableStateOf(true) }
        var generation by remember { mutableIntStateOf(0) }
        var localStatus by remember { mutableStateOf("Original saved map is intact: ${store.load().any { it == saved }}") }
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("LOCAL REVIEW — Unpublish", style = MaterialTheme.typography.titleLarge)
                Text("Persistent local fixture only. No HTTP, Firebase or production data.")
                Text("Successful unpublish is confirmed after 800ms. Rejected unpublish always returns an error.")
                Text(localStatus)
                Text(saved.name)
                Button(onClick = { generation++; opened = true }) { Text("Reopen Community Maps") }
                OutlinedButton(onClick = { localStatus = "Original saved map is intact: ${store.load().any { it == saved }}" }) { Text("Check original saved map") }
                OutlinedButton(onClick = {
                    preferences.edit().putBoolean("published", true).apply()
                    generation++; opened = true
                }) { Text("Reset only local publication fixture") }
            }
        }
        if (opened) key(generation) { CommunityMapsScreen(gateway, true, { "local-owner-fixture" },
            { _, _ -> }, {
                opened = false
                localStatus = "Original saved map is intact: ${store.load().any { it == saved }}"
            }) }
    } } }
}
